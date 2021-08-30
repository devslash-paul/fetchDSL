package net.devslash

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.content.*
import io.ktor.http.*

object KtorRequestMapper {
  private val mapper = ObjectMapper()

  fun mapHttpToKtor(httpRequest: HttpRequest): HttpRequestBuilder {
    return HttpRequestBuilder().apply {
      url.takeFrom(httpRequest.url)
      method = mapType(httpRequest.type)
      headers {
        httpRequest.headers.forEach {
          it.value.forEach { kVal ->
            append(it.key, kVal)
          }
        }
      }
      when (val reqBody = httpRequest.body) {
        EmptyBody -> {
        }
        is BytesRequestBody -> {
          body = ByteArrayContent(reqBody.body.readAllBytes())
        }
        is StringRequestBody -> {
          body = ByteArrayContent(reqBody.body.toByteArray())
        }
        is FormRequestBody -> {
          body = FormDataContent(
              parametersOf(*reqBody.form.map { (key, value) -> key to value }.toTypedArray())
          )
        }
        is JsonRequestBody -> {
          body = TextContent(
              mapper.writeValueAsString(reqBody.data),
              ContentType.Application.Json
          )
        }
        is MultipartFormRequestBody -> {
          body = MultiPartFormDataContent(formData {
            reqBody.form.forEach {
              when (val value = it.value) {
                is NumberType -> append(it.key, value.i)
                is StringType -> append(it.key, value.i)
                is ByteArrayType -> append(it.key, value.i)
              }
            }
          })
        }
      }
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

}
