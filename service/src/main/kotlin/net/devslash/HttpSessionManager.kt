package net.devslash

import awaitByteArrayResponse
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.getOrElse
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.Semaphore

class HttpSessionManager(private val session: Session) : SessionManager {

  private val manager = FuelManager()
  private val semaphore = Semaphore(0)
  private lateinit var sessionManager: SessionManager

  fun run() {
    sessionManager = this
    semaphore.release(session.concurrency)
    session.calls.map { call(it, CookieJar()) }
  }

  @ExperimentalCoroutinesApi
  suspend fun produceHttp(
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
    val channel: Channel<Pair<HttpRequest, RequestData>> = Channel(session.concurrency)
    launch { produceHttp(call, jar, channel) }

    val postRequest: MutableList<PostHook> = mutableListOf(jar)
    postRequest.addAll(call.postHooks)

    while (!(channel.isClosedForReceive)) {
      for (next in channel) {
        try {
          while (!semaphore.tryAcquire()) {
            delay(10)
          }

          launch(Dispatchers.IO) {
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
          }
        } finally {
          semaphore.release()
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
    val currentBody = body.get()
    val type = call.type

    val req = HttpRequest(mapType(type), currentUrl, currentBody)
    return req
  }

  private fun mapResponse(makeRequest: Pair<Response, Result<ByteArray, FuelError>>): HttpResponse {
    val (resp, res) = makeRequest
    res.fold({}, { error ->
      // If we have an error, for soe reason the details show up here
      return HttpResponse(
        error.response.url, error.response.statusCode, error.response.headers, error.response.data
      )
    })
    return HttpResponse(resp.url, resp.statusCode, resp.headers, res.getOrElse(byteArrayOf()))
  }

  private fun mapType(type: HttpMethod): Method {
    return when (type) {
      HttpMethod.GET -> Method.GET
      HttpMethod.POST -> Method.POST
    }
  }

  private suspend fun makeRequest(modelRequest: HttpRequest): Pair<Response, Result<ByteArray, FuelError>> {
    val req = manager.request(modelRequest.type, modelRequest.url).allowRedirects(false)
    req.header(modelRequest.headers)
    req.body(modelRequest.body)

    val (_, response, result) = req.awaitByteArrayResponse()
    return Pair(response, result)
  }
}
