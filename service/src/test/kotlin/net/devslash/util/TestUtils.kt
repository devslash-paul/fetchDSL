package net.devslash.util

import net.devslash.*
import java.net.URI

fun requestDataFromList(listOf: List<String>? = null): RequestData {
  return object : RequestData {
    override fun <T> visit(visitor: RequestVisitor<T, Any?>): T {
      return visitor(listOf, List::class.java)
    }
  }
}

fun getBasicRequest(): HttpRequest {
  return HttpRequest(HttpMethod.GET, "https://example.com", EmptyBodyProvider)
}

fun getCookieJar(): CookieJar {
  return CookieJar()
}

fun getSessionManager(): SessionManager {
  return HttpSessionManager(HttpDriver(ConfigBuilder().build()), getSession())
}

fun getSession(): Session {
  return SessionBuilder().build()
}

fun getResponseWithBody(body: ByteArray): HttpResponse {
  return HttpResponse(URI("http://example.com"), 200, mapOf(), body)
}

fun getBasicResponse(): HttpResponse {
  return getResponseWithBody("Body".toByteArray())
}

fun getCall(sup: HttpBody? = null, url: String = "https://example.com") = CallBuilder<Any?>(
  url
).apply {
  body = sup
}.build()
