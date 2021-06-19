package net.devslash.util

import net.devslash.*
import java.net.URI

fun requestDataFromList(listOf: List<String>? = null): RequestData {
  return object : RequestData() {
    override fun <T> visit(visitor: RequestVisitor<T, Any?>): T {
      return visitor(listOf, List::class.java)
    }
  }
}

fun basicData() : RequestData {
  return ListRequestData(listOf<String>())
}

fun basicRequest(): HttpRequest {
  return HttpRequest(HttpMethod.GET, "https://example.com", EmptyBodyProvider)
}

fun getCookieJar(): CookieJar {
  return CookieJar()
}

fun getSessionManager(): SessionManager {
  val config = ConfigBuilder().build()
  val ktor = KtorClientAdapter(config)
  return HttpSessionManager(HttpDriver(ktor), getSession())
}

fun getSession(): Session {
  return SessionBuilder().build()
}

fun getResponseWithBody(body: ByteArray): HttpResponse {
  return HttpResponse(URI("http://example.com"), 200, mapOf(), body)
}

fun basicResponse(): HttpResponse {
  return getResponseWithBody("Body".toByteArray())
}

fun getCall(sup: HttpBody? = null, url: String = "https://example.com") = CallBuilder<Any?>(
  url
).apply {
  body = sup
}.build()