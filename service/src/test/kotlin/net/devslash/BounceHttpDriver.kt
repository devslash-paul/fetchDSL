package net.devslash

import net.devslash.util.basicUrl
import java.net.URI

class BounceHttpDriver(
  private val resp: HttpResponse = HttpResponse(URI(basicUrl), 200, mapOf(), ByteArray(2))
) :
  Driver {
  override suspend fun call(req: HttpRequest): HttpResult<HttpResponse, Exception> {
    return Success(resp)
  }

  override fun close() {
  }
}