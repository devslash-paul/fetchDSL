package net.devslash

import awaitByteArrayResponse
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.getOrElse
import kotlinx.coroutines.*
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore

class HttpSessionManager(private val session: Session) : SessionManager {

  private val manager = FuelManager()
  private val semaphore = Semaphore(0)
  private lateinit var sessionManager: SessionManager

  fun run(): List<List<Job>> {
    sessionManager = this
    semaphore.release(session.concurrency)
    return session.calls.map { call(it, CookieJar()) }
  }

  override fun call(call: Call): MutableList<Job> = call(call, CookieJar())
  override fun call(call: Call, jar: CookieJar): MutableList<Job> {
    // Okay, so in here we're going to do the one to many calls we have to to get this to run.
    val requests = mutableListOf<Job>()
    runBlocking {
      // Call the prerequesites
      val ctx = Executors.newFixedThreadPool(session.concurrency).asCoroutineDispatcher()
      val dataSupplier = getCallDataSupplier(call.dataSupplier)
      val preRequest = call.preHooks.toMutableList()
      preRequest.add(jar)
      val postRequest: MutableList<PostHook> = mutableListOf(jar)
      postRequest.addAll(call.postHooks)

      do {
        val data = dataSupplier.getDataForRequest()
        val body = getBodyProvider(call, data)
        val url = getUrlProvider(call, data)

        val currentUrl = url.get()
        val currentBody = body.get()
        val type = call.type

        val req = HttpRequest(mapType(type), currentUrl, currentBody)
        val shouldSkip =
            preRequest.filter { it is SkipPreHook }.any { (it as SkipPreHook).skip(data) }

        if (shouldSkip) {
          continue
        }
        preRequest.forEach {
          when (it) {
            is SimplePreHook -> it.accept(req, data)
            is SessionPersistingPreHook -> it.accept(sessionManager, jar, req, data)
          }
        }
        // Take out later
        call.headers?.forEach { req.addHeader(it.first, it.second.get(data)) }

        while (!semaphore.tryAcquire()) {
          delay(100)
        }

        requests.add(async(ctx) {
          try {
            val request = makeRequest(req)
            val resp = mapResponse(request)
            postRequest.forEach {
              when (it) {
                is SimplePostHook -> it.accept(resp.copy())
                is ChainReceivingResponseHook -> it.accept(resp)
                is FullDataPostHook -> it.accept(req, resp, data)
              }
            }

            call.output.forEach {
              when (it) {
                is BasicOutput -> it.accept(resp, data)
              }
            }
          } catch (e: Exception) {
            println(e)
          } finally {
            semaphore.release()
          }
        })
      } while (dataSupplier.hasNext())
    }

    return requests
  }

  private fun mapResponse(makeRequest: Pair<Response, Result<ByteArray, FuelError>>): HttpResponse {
    val (resp, res) = makeRequest
    res.fold({}, { error ->
      // If we have an error, for soe reason the details show up here
      return HttpResponse(error.response.url, error.response.statusCode, error.response.headers,
          error.response.data)
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
//    val (_, response, result) = req.response()
//    result.fold({}, {it ->println(it) })
    return Pair(response, result)
  }
}
