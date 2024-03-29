package net.devslash.util

import net.devslash.*
import java.net.URI

fun requestDataFromList(listOf: List<String>? = null): RequestData<List<String>> {
  return object : RequestData<List<String>>() {
    override fun <T> visit(visitor: RequestVisitor<T, Any?>): T {
      return visitor(listOf, List::class.java)
    }

    override fun get(): List<String> = listOf!!
  }
}

fun basicData(): RequestData<*> {
  return ListRequestData(listOf<String>())
}

const val basicUrl: String = "https://example.com"
fun basicRequest(): HttpRequest {
  return HttpRequest(HttpMethod.GET, basicUrl, EmptyBody)
}

fun getCookieJar(): CookieJar {
  return DefaultCookieJar()
}

fun <T> getCallRunner(): CallRunner<T> {
  return { _: Call<T> -> null }
}

fun getUntypedCallRunner(): CallRunner<*> {
  return { _: Call<*> -> null }
}


fun getSessionManager(): SessionManager {
  val config = ConfigBuilder().build()
  val ktor = KtorClientAdapter(config)
  return HttpSessionManager(HttpDriver(ktor))
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

fun getCall(sup: HttpBody<List<String>>? = null, url: String = "https://example.com"): Call<List<String>> = CallBuilder<List<String>>(
    url
).apply {
  body = sup
}.build()

fun <T> getTypedCall(sup: HttpBody<T>? = null, url: String = "https://example.com"): Call<T> = CallBuilder<T>(
    url
).apply {
  body = sup
}.build()
