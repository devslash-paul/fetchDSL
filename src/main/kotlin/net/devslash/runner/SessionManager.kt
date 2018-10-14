package net.devslash.runner

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.getOrElse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import net.devslash.*
import java.io.File
import java.util.concurrent.Semaphore
import kotlin.coroutines.coroutineContext


class SessionManager(private val session: Session) {
  private val manager = FuelManager()
  private val semaphore = Semaphore(0)

  suspend fun run() {
    semaphore.release(session.concurrency)
    session.calls.forEach { call(it) }
  }

  suspend fun call(call: Call, jar: CookieJar = CookieJar()): MutableList<Job> {
    // Call the prerequesites
    val dataSupplier = getCallDataSupplier(call.dataSupplier)
    val preRequest = call.preHooks.toMutableList()
    preRequest.add(jar)
    val postRequest: MutableList<PostHook> = mutableListOf(jar)
    postRequest.addAll(call.postHooks)

    // Okay, so in here we're going to do the one to many calls we have to to get this to run.
    val requests = mutableListOf<Job>()
    do {
      val data: RequestData = dataSupplier.getDataForRequest()
      val body = getBodyProvider(call, data)
      val url = getUrlProvider(call, data)
      val output = getOutputHandler(call, data)

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
          is SessionPersistingPreHook -> it.accept(this, jar, req, data)
        }
      }
      // Take out later
      call.headers?.forEach { req.addHeader(it.first, it.second.get(data)) }

      semaphore.acquire()
      requests.add(CoroutineScope(coroutineContext).async {
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
          output.output(resp, data)
        } catch (e: Exception) {
          println(e)
        } finally {
          semaphore.release()
        }
      })
    } while (dataSupplier.hasNext())

    return requests
  }

  private fun mapResponse(makeRequest: Pair<Response, Result<ByteArray, FuelError>>): HttpResponse {
    val (resp, res) = makeRequest

    return HttpResponse(resp.url, resp.statusCode, resp.headers, res.getOrElse(byteArrayOf()))
  }

  private fun mapType(type: HttpMethod): Method {
    return when (type) {
      HttpMethod.GET -> Method.GET
      HttpMethod.POST -> Method.POST
    }
  }

  private fun makeRequest(modelRequest: HttpRequest): Pair<Response, Result<ByteArray, FuelError>> {
    val req = manager.request(modelRequest.type, modelRequest.url).allowRedirects(false)
    req.header(modelRequest.headers)
    req.body(modelRequest.body)

    val (_, response, result) = req.response()
    return Pair(response, result)
  }
}
