package net.devslash

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.content.*
import io.ktor.http.*
import io.ktor.util.cio.*
import java.net.URL
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

internal class HttpDriver(config: Config) : AutoCloseable {

  private val client = HttpClient(Apache) {
    engine {
      connectTimeout = config.connectTimeout
      connectionRequestTimeout = config.connectionRequestTimeout
      socketTimeout = config.socketTimeout
      followRedirects = config.followRedirects
    }
    followRedirects = config.followRedirects
  }
  private val mapper = ObjectMapper()

  suspend fun call(req: HttpRequest): HttpResult<io.ktor.client.response.HttpResponse, java.lang.Exception> {
    try {
      val resp = client.call(req.url) {
        method = mapType(req.type)
        headers {
          req.headers.forEach {
            it.value.forEach { kVal ->
              append(it.key, kVal)
            }
          }
        }
        when (req.body) {
          is JsonBody -> {
            val text = (req.body as JsonBody).get()
            body = TextContent(
              mapper.writeValueAsString(text),
              ContentType.Application.Json
            )
          }
          is BasicBodyProvider<*> -> {
            body = ByteArrayContent((req.body as BasicBodyProvider<*>).get().toByteArray())
          }
          is FormBody<*> -> body = FormDataContent(Parameters.build {
            val prov = req.body as FormBody<*>
            prov.get().forEach { (key, value) ->
              value.forEach {
                append(key, it)
              }
            }
          })
        }
      }
      return Success(resp.response)
    } catch (e: Exception) {
      return Failure(e)
    }
  }

  private fun mapType(type: HttpMethod): io.ktor.http.HttpMethod {
    return when (type) {
      HttpMethod.GET -> io.ktor.http.HttpMethod.Get
      HttpMethod.POST -> io.ktor.http.HttpMethod.Post
      HttpMethod.DELETE -> io.ktor.http.HttpMethod.Delete
      HttpMethod.PUT -> io.ktor.http.HttpMethod.Put
      HttpMethod.PATCH -> io.ktor.http.HttpMethod.Patch
      HttpMethod.HEAD -> io.ktor.http.HttpMethod.Head
      HttpMethod.OPTIONS -> io.ktor.http.HttpMethod.Options
    }
  }

  internal suspend fun mapResponse(request: io.ktor.client.response.HttpResponse): HttpResponse {
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

  override fun close() {
    client.close()
  }
}
