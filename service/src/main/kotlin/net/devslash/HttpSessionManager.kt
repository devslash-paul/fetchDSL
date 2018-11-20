package net.devslash

import io.ktor.client.HttpClient
import io.ktor.client.call.call
import io.ktor.client.engine.HttpClientEngine
import io.ktor.http.Headers
import io.ktor.http.headersOf
import io.ktor.util.cio.toByteArray
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore

class HttpSessionManager(private val engine: HttpClientEngine, private val session: Session) :
  SessionManager {

  private val semaphore = Semaphore(0)
  private lateinit var sessionManager: SessionManager
  private val client = HttpClient(engine) {
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
      val preRequest = call.preHooks.toMutableList()
      preRequest.add(jar)

      val shouldSkip =
        preRequest.filter { it is SkipPreHook }.any { (it as SkipPreHook).skip(data) }
      if (shouldSkip) continue

      preRequest.forEach {
        when (it) {
          is SimplePreHook -> it.accept(req, data)
          is SessionPersistingPreHook -> it.accept(sessionManager, jar, req, data)
        }
      }
      call.headers?.forEach { req.addHeader(it.first, it.second.get(data)) }

      channel.send(Pair(req, data))
    } while (dataSupplier.hasNext())
    channel.close()
  }

  override fun call(call: Call) = call(call, CookieJar())

  @ExperimentalCoroutinesApi
  override fun call(call: Call, jar: CookieJar) = runBlocking {
    // Okay, so in here we're going to do the one to many calls we have to to get this to run.
    val channel: Channel<Pair<HttpRequest, RequestData>> = Channel(session.concurrency * 2)

    val x = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    launch(x) { produceHttp(call, jar, channel) }

    val ex = Executors.newFixedThreadPool(8 * 2).asCoroutineDispatcher()
    val postRequest: MutableList<PostHook> = mutableListOf(jar)
    postRequest.addAll(call.postHooks)

    semaphore.release(session.concurrency)

    while (!(channel.isClosedForReceive)) {
      for (next in channel) {
        while (!semaphore.tryAcquire()) {
          delay(10)
        }
        launch(ex) {
          try {
            val request = makeRequest(next.first)
            val resp = mapResponse(request)
            postRequest.forEach {
              when (it) {
                is SimplePostHook -> it.accept(resp.copy())
                is ChainReceivingResponseHook -> it.accept(resp)
                is FullDataPostHook -> it.accept(next.first, resp, next.second)
              }
            }
            call.output.forEach {
              when (it) {
                is BasicOutput -> it.accept(resp, next.second)
              }
            }

          } finally {
            semaphore.release()
          }
        }
      }
    }

    ex.close()
    x.close()
  }

  private fun getHttpRequest(
    call: Call, data: RequestData
  ): HttpRequest {
    val url = getUrlProvider(call, data)
    val body = getBodyProvider(call, data)
    val currentUrl = url.get()
    val currentBody = body.get()
    val type = call.type

    val req = HttpRequest(type, currentUrl, currentBody)
    return req
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
      headersOf(*modelRequest.headers.map { Pair(it.key, it.value) }.toTypedArray())
      body = modelRequest.body
    }

    val resp = req.response
    return resp
  }
}
