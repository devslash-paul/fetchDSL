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
import java.net.URL

class HttpDriver(http: HttpClientEngine, followRedirects: Boolean = false) : AutoCloseable {

  private val client = HttpClient(http) {
    this.followRedirects = followRedirects
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
          is JsonBody          -> {
            body = TextContent(mapper.writeValueAsString((req.body as JsonBody).get()),
                ContentType.Application.Json)
          }
          is BasicBodyProvider -> {
            body = ByteArrayContent((req.body as BasicBodyProvider).get().toByteArray())
          }
          is FormBody          -> body = FormDataContent(Parameters.build {
            val prov = req.body as FormBody
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
      HttpMethod.GET  -> io.ktor.http.HttpMethod.Get
      HttpMethod.POST -> io.ktor.http.HttpMethod.Post
      HttpMethod.DELETE -> io.ktor.http.HttpMethod.Delete
      HttpMethod.PUT -> io.ktor.http.HttpMethod.Put
      HttpMethod.PATCH -> io.ktor.http.HttpMethod.Patch
      HttpMethod.HEAD -> io.ktor.http.HttpMethod.Head
      HttpMethod.OPTIONS -> io.ktor.http.HttpMethod.Options
    }
  }

  suspend fun mapResponse(request: io.ktor.client.response.HttpResponse): HttpResponse {
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

  override fun close() {
    client.close()
  }
}
