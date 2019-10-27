package net.devslash.util

import io.ktor.client.engine.apache.Apache
import net.devslash.*
import java.net.URL

fun requestDataFromList(listOf: List<String>? = null): RequestData {
  return object : RequestData {
    override fun getReplacements(): Map<String, String> {
      if (listOf != null) {
        return listOf.mapIndexed { i, p ->
          "!${i + 1}!" to p
        }.toMap()
      }

      return mapOf()
    }
  }
}

fun getBasicRequest() : HttpRequest {
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

fun getResponseWithBody(body: ByteArray) : HttpResponse {
  return HttpResponse(URL("http://example.com"), 200, mapOf(), body)
}

fun getResponse(): HttpResponse {
  return getResponseWithBody("Body".toByteArray())
}

fun getCall(sup: HttpBody? = null, url: String = "https://example.com") = CallBuilder(
  url
).apply {
  body = sup
}.build()
