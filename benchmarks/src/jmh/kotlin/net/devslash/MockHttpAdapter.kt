package net.devslash

import kotlinx.coroutines.delay
import java.net.URI

class MockHttpAdapter : HttpClientAdapter {
  val resp = HttpResponse(URI("http://example.com"), 200, mapOf(), byteArrayOf())
  override suspend fun request(httpRequest: HttpRequest): HttpResponse {
    delay(50)
    return resp
  }

  override fun close() = Unit
}
