package net.devslash.util

import net.devslash.*
import java.net.URL

fun getBasicRequest() : HttpRequest {
  return HttpRequest(HttpMethod.GET, "https://example.com", EmptyBodyProvider)
}

fun getCookieJar(): CookieJar {
  return CookieJar()
}

fun getResponseWithBody(body: ByteArray) : HttpResponse {
  return HttpResponse(URL("http://example.com"), 200, mapOf(), body)
}

fun getResponse(): HttpResponse {
  return getResponseWithBody("Body".toByteArray())
}

fun <T> getCall(sup: HttpBody<T>? = null, url: String = "https://example.com") = CallBuilder<T>(
  url
).apply {
  body = sup
}.build()
