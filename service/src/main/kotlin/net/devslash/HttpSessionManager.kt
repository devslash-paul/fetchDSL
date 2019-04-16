package net.devslash

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.call.call
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.headers
import io.ktor.content.ByteArrayContent
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.Parameters
import io.ktor.util.cio.toByteArray
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import kotlin.coroutines.CoroutineContext

typealias Contents = Pair<HttpRequest, RequestData>

class HttpSessionManager(engine: HttpClientEngine, private val session: Session) : SessionManager {

  private val semaphore = Semaphore(0)
  private val mapper = ObjectMapper()
  private lateinit var sessionManager: SessionManager
  private val client = HttpClient(engine) {
    followRedirects = false
    engine {

    }
  }

  fun run() {
    sessionManager = this
    session.calls.map { call(it, CookieJar()) }

    client.close()
  }

  private suspend fun produceHttp(call: Call,
                                  jar: CookieJar, channel: Channel<Envelope<Contents>>) {
    val dataSupplier = getCallDataSupplier(call.dataSupplier)
    do {
      val data = dataSupplier.getDataForRequest()
      val req = getHttpRequest(call, data)
      // Call the prerequesites
      val preRequest = call.beforeHooks.toMutableList()
      preRequest.add(jar)

      val shouldSkip =
          preRequest.filter { it is SkipBeforeHook }.any { (it as SkipBeforeHook).skip(data) }
      if (shouldSkip) continue

      preRequest.forEach {
        when (it) {
          is SimpleBeforeHook            -> it.accept(req, data)
          is SessionPersistingBeforeHook -> it.accept(sessionManager, jar, req, data)
        }
      }
      call.headers?.forEach { entry ->
        entry.value.forEach {
          val s = when (it) {
            is StrValue      -> it.value
            is ProvidedValue -> it.lambda(data)
          }
          req.addHeader(entry.key, s)
        }
      }

      channel.send(Envelope(Pair(req, data)))
    } while (dataSupplier.hasNext())
    channel.close()
  }

  override fun call(call: Call) = call(call, CookieJar())

  private suspend fun getTicket() {
    withContext(Dispatchers.Default) {
      semaphore.acquire()
    }
  }

  @ExperimentalCoroutinesApi
  override fun call(call: Call, jar: CookieJar) = runBlocking {
    // Okay, so in here we're going to do the one to many calls we have to to get this to run.
    val channel: Channel<Envelope<Pair<HttpRequest, RequestData>>> =
        Channel(session.concurrency * 2)
    val produceThreadPool = System.getProperty("PRODUCE_THREAD_POOL_SIZE")?.toInt()
        ?: Runtime.getRuntime().availableProcessors()
    val produceDispatcher = Executors.newFixedThreadPool(produceThreadPool).asCoroutineDispatcher()
    launch(produceDispatcher) { produceHttp(call, jar, channel) }

    val afterRequest: MutableList<AfterHook> = mutableListOf(jar)
    afterRequest.addAll(call.afterHooks)

    val threadPool = System.getProperty("HTTP_THREAD_POOL_SIZE")?.toInt()
        ?: Runtime.getRuntime().availableProcessors() * 2
    val dispatcher = Executors.newFixedThreadPool(threadPool)
        .asCoroutineDispatcher()
    semaphore.release(session.concurrency)

    val jobs = mutableListOf<Job>()
    repeat(session.concurrency) {
      jobs += launchHttpProcessor(call,
          afterRequest,
          it,
          channel,
          dispatcher)
    }
    jobs.joinAll()
  }

  private fun CoroutineScope.launchHttpProcessor(call: Call,
                                                 afterRequest: List<AfterHook>,
                                                 it: Int,
                                                 channel: Channel<Envelope<Pair<HttpRequest, RequestData>>>,
                                                 dispatcher: CoroutineContext) = launch(dispatcher) {
    for (next in channel) {
      // ensure that this is a valid request
      if (next.shouldProceed()) {
        val contents = next.get()
        when (val request = makeRequest(contents.first)) {
          is Failure -> {
            handleFailure(call, channel, next, request)
          }
          is Success -> {
            val resp = mapResponse(request.value)
            afterRequest.forEach {
              when (it) {
                is SimpleAfterHook -> it.accept(resp.copy())
                is ChainReceivingResponseHook -> it.accept(resp)
                is FullDataAfterHook -> it.accept(contents.first, resp, contents.second)
                }
              }
            }
          }
        }
    }
  }

  private suspend fun handleFailure(call: Call,
                                    channel: Channel<Envelope<Pair<HttpRequest, RequestData>>>,
                                    next: Envelope<Pair<HttpRequest, RequestData>>,
                                    request: Failure<*, java.lang.Exception>) {
    call.onError?.let {
      when (it) {
        is ChannelReceiving<*> -> {
          (it as ChannelReceiving<Contents>).accept(channel, next, request.err)
        }
        else                   -> {
          throw request.err
        }
      }
    }
  }

  private fun getHttpRequest(call: Call, data: RequestData): HttpRequest {
    val url = getUrlProvider(call, data)
    val body = getBodyProvider(call, data)
    val currentUrl = url.get()
    val type = call.type

    return HttpRequest(type, currentUrl, body)
  }

  private suspend fun mapResponse(request: io.ktor.client.response.HttpResponse): HttpResponse {
    val response = request.call.response
    return HttpResponse(URL(request.call.request.url.toString()),
        response.status.value,
        mapHeaders(response.headers),
        response.content.toByteArray())
  }

  private fun mapHeaders(headers: Headers): Map<String, List<String>> {
    val map = mutableMapOf<String, List<String>>()
    headers.forEach { key, value -> map[key] = value }

    return map
  }

  private fun mapType(type: HttpMethod): io.ktor.http.HttpMethod {
    return when (type) {
      HttpMethod.GET  -> io.ktor.http.HttpMethod.Get
      HttpMethod.POST -> io.ktor.http.HttpMethod.Post
    }
  }

  private suspend fun makeRequest(modelRequest: HttpRequest): Result<io.ktor.client.response.HttpResponse, java.lang.Exception> {
    try {
      val req = client.call(modelRequest.url) {
        method = mapType(modelRequest.type)
        headers {
          modelRequest.headers.forEach {
            it.value.forEach { kVal ->
              append(it.key, kVal)
            }
          }
        }
        when (modelRequest.body) {
          is JsonBody          -> {
            body = TextContent(mapper.writeValueAsString((modelRequest.body as JsonBody).get()),
                ContentType.Application.Json)
          }
          is BasicBodyProvider -> {
            body = ByteArrayContent((modelRequest.body as BasicBodyProvider).get().toByteArray())
          }
          is FormBody          -> body = FormDataContent(Parameters.build {
            val prov = modelRequest.body as FormBody
            prov.get().forEach { key, value ->
              value.forEach {
                append(key, it)
              }
            }
          })
        }
      }
      return Success(req.response)
    } catch (e: Exception) {
      return Failure(e)
    }
  }
}


