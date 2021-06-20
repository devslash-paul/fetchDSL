package net.devslash.util

import net.devslash.*
import java.net.URI

class BounceHttpDriver(
  private val resp: HttpResponse = HttpResponse(URI(basicUrl), 200, mapOf(), ByteArray(2))
) :
  Driver {
  override suspend fun call(req: HttpRequest): HttpResult<HttpResponse, Exception> = Success(resp)
  override fun close(): Unit = Unit
}
