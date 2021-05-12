package net.devslash

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.*
import io.ktor.client.request.forms.FormDataContent
import io.ktor.content.ByteArrayContent
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.Parameters
import io.ktor.util.*
import java.net.URL
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.forEach
import kotlin.collections.mutableMapOf
import kotlin.collections.set

public class HttpDriver(config: Config) : AutoCloseable {

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

  public suspend fun call(req: HttpRequest): HttpResult<io.ktor.client.statement.HttpResponse, java.lang.Exception> {
    try {
      val resp = client.request<io.ktor.client.statement.HttpResponse>(req.url) {
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
            body = TextContent(mapper.writeValueAsString((req.body as JsonBody).get()),
                    ContentType.Application.Json)
          }
          is BasicBodyProvider -> {
            body = ByteArrayContent((req.body as BasicBodyProvider).get().toByteArray())
          }
          is FormBody -> body = FormDataContent(Parameters.build {
            val prov = req.body as FormBody
            prov.get().forEach { (key, value) ->
              value.forEach {
                append(key, it)
              }
            }
          })
        }
      }
      return Success(resp)
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

  public suspend fun mapResponse(response: io.ktor.client.statement.HttpResponse): HttpResponse {
    val response = response.call.response
    return HttpResponse(URL(response.call.request.url.toString()),
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
