package net.devslash.util

import net.devslash.HttpClientAdapter
import net.devslash.HttpRequest
import net.devslash.HttpResponse

class FailingClientAdapter : HttpClientAdapter {
  override suspend fun request(httpRequest: HttpRequest): HttpResponse = throw RuntimeException()
  override fun close(): Unit = Unit
}
