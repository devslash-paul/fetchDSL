package net.devslash

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.content.*
import io.ktor.http.*
import io.ktor.util.*
import java.net.URI
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

class HttpDriver(config: Config) : Driver {

  private val client = HttpClient(Apache) {
    engine {
      connectTimeout = config.connectTimeout
      connectionRequestTimeout = config.connectionRequestTimeout
      socketTimeout = config.socketTimeout
      followRedirects = config.followRedirects
    }
    expectSuccess = false
    followRedirects = config.followRedirects
  }
  private val mapper = ObjectMapper()

  override suspend fun call(req: HttpRequest): HttpResult<HttpResponse, java.lang.Exception> {
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
            body = TextContent(
              mapper.writeValueAsString((req.body as JsonBody).get()),
              ContentType.Application.Json
            )
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
          is MultipartForm -> {
            val bd = req.body as MultipartForm
            body = MultiPartFormDataContent(formData {
              bd.parts.forEach {
                when (val value = it.value) {
                  is NumberType -> append(it.key, value.i)
                  is StringType -> append(it.key, value.i)
                  is ByteArrayType -> append(it.key, value.i)
                }
              }
            })
          }
          EmptyBodyProvider -> {/* Do nothing explicitly */
          }
        }
      }
      return Success(mapResponse(resp))
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

  private suspend fun mapResponse(response: io.ktor.client.statement.HttpResponse): HttpResponse {
    val inlineResp = response.call.response
    return HttpResponse(
      URI(inlineResp.call.request.url.toString()),
      inlineResp.status.value,
      mapHeaders(inlineResp.headers),
      inlineResp.content.toByteArray()
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
