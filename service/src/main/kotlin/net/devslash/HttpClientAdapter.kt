package net.devslash

interface HttpClientAdapter : AutoCloseable {
  suspend fun request(httpRequest: HttpRequest): HttpResponse
}