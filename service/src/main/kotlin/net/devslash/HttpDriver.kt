package net.devslash

class HttpDriver(private val clientAdapter: HttpClientAdapter) : Driver {
  override suspend fun call(req: HttpRequest): HttpResult<HttpResponse, java.lang.Exception> {
    return try {
      val resp = clientAdapter.request(req)
      Success(resp)
    } catch (e: Exception) {
      Failure(e)
    }
  }


  override fun close() {
    clientAdapter.close()
  }
}
