package net.devslash

import io.ktor.client.HttpClient
import io.ktor.client.call.call
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.headers
import io.ktor.content.ByteArrayContent
import io.ktor.http.Headers
import io.ktor.http.Parameters
import io.ktor.util.cio.toByteArray
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.net.URL
import java.util.concurrent.Semaphore

class HttpSessionManager(engine: HttpClientEngine, private val session: Session) :
  SessionManager {

  private val semaphore = Semaphore(0)
  private lateinit var sessionManager: SessionManager
  private val client = HttpClient(Apache) {
    followRedirects = false
  }

  fun run() {
    sessionManager = this
    session.calls.map { call(it, CookieJar()) }

    client.close()
  }

  private suspend fun produceHttp(
    call: Call, jar: CookieJar, channel: Channel<Pair<HttpRequest, RequestData>>
  ) {
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
          val s = when(it) {
            is StrValue -> it.value
            is ProvidedValue -> it.lambda(data)
          }
          req.addHeader(entry.key, s)
        }
      }

      channel.send(Pair(req, data))
    } while (dataSupplier.hasNext())
    channel.close()
  }

  override fun call(call: Call) = call(call, CookieJar())

  @ExperimentalCoroutinesApi
  override fun call(call: Call, jar: CookieJar) = runBlocking {
    // Okay, so in here we're going to do the one to many calls we have to to get this to run.
    val channel: Channel<Pair<HttpRequest, RequestData>> = Channel(session.concurrency * 2)
    launch(Dispatchers.Default) { produceHttp(call, jar, channel) }

    val afterRequest: MutableList<AfterHook> = mutableListOf(jar)
    afterRequest.addAll(call.afterHooks)

    semaphore.release(session.concurrency)

    while (!(channel.isClosedForReceive)) {
      for (next in channel) {
        while (!semaphore.tryAcquire()) {
          delay(10)
        }
        launch(Dispatchers.IO) {
          try {
            val request = makeRequest(next.first)
            val resp = mapResponse(request)
            afterRequest.forEach {
              when (it) {
                is SimpleAfterHook            -> it.accept(resp.copy())
                is ChainReceivingResponseHook -> it.accept(resp)
                is FullDataAfterHook          -> it.accept(next.first, resp, next.second)
              }
            }
          } finally {
            semaphore.release()
          }
        }
      }
    }
  }

  private fun getHttpRequest(
    call: Call, data: RequestData
  ): HttpRequest {
    val url = getUrlProvider(call, data)
    val body = getBodyProvider(call, data)
    val currentUrl = url.get()
    val type = call.type

    return HttpRequest(type, currentUrl, body)
  }

  private suspend fun mapResponse(request: io.ktor.client.response.HttpResponse): HttpResponse {
    val response = request.call.response
    return HttpResponse(
      URL(request.call.request.url.toString()),
      response.status.value,
      mapHeaders(response.headers),
      response.content.toByteArray()
    )
  }

  private fun mapHeaders(headers: Headers): Map<String, List<String>> {
    val map = mutableMapOf<String, List<String>>()
    headers.forEach { key, value -> map[key] = value }

    return map
  }

  private fun mapType(type: HttpMethod): io.ktor.http.HttpMethod {
    return when (type) {
      HttpMethod.GET -> io.ktor.http.HttpMethod.Get
      HttpMethod.POST -> io.ktor.http.HttpMethod.Post
    }
  }

  private suspend fun makeRequest(modelRequest: HttpRequest): io.ktor.client.response.HttpResponse {
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
        is BasicBodyProvider -> {
          body = ByteArrayContent((modelRequest.body as BasicBodyProvider).get().toByteArray())
        }
        is FormBody -> body = FormDataContent(Parameters.build {
          val prov = modelRequest.body as FormBody
          prov.get().forEach { key, value ->
            value.forEach {
              append(key, it)
            }
          }
        })
      }
    }
    return req.response
  }
}
