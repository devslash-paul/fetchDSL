package net.devslash

interface Driver : AutoCloseable {
  suspend fun call(req: HttpRequest): HttpResult<HttpResponse, Exception>
}