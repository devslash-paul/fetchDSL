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

class KtorClientAdapter(config: Config) : HttpClientAdapter {
  private val mapper = ObjectMapper()

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

  override suspend fun request(httpRequest: HttpRequest): HttpResponse {
    val clientResp = client.request<io.ktor.client.statement.HttpResponse>(httpRequest.url) {
      method = mapType(httpRequest.type)
      headers {
        httpRequest.headers.forEach {
          it.value.forEach { kVal ->
            append(it.key, kVal)
          }
        }
      }
      when (httpRequest.body) {
        is JsonBody -> {
          body = TextContent(
            mapper.writeValueAsString((httpRequest.body as JsonBody).get()),
            ContentType.Application.Json
          )
        }
        is BasicBodyProvider -> {
          body = ByteArrayContent((httpRequest.body as BasicBodyProvider).get().toByteArray())
        }
        is FormBody -> body = FormDataContent(Parameters.build {
          val prov = httpRequest.body as FormBody
          prov.get().forEach { (key, value) ->
            value.forEach {
              append(key, it)
            }
          }
        })
        is MultipartForm -> {
          val bd = httpRequest.body as MultipartForm
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
    return mapResponse(clientResp)
  }

  override fun close() {
    client.close()
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
}