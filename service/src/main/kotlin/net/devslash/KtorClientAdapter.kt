package net.devslash

import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.request.*

typealias KtorResponse = io.ktor.client.statement.HttpResponse

class KtorClientAdapter(config: Config) : HttpClientAdapter {
  private val client = HttpClient(Apache) {
    engine {
      connectTimeout = config.connectTimeout
      connectionRequestTimeout = config.connectionRequestTimeout
      socketTimeout = config.socketTimeout
      followRedirects = config.followRedirects
    }
    expectSuccess = false
    followRedirects = config.followRedirects
  }

  override suspend fun request(httpRequest: HttpRequest): HttpResponse {
    val clientResp = client.request<KtorResponse>(KtorRequestMapper.mapHttpToKtor(httpRequest))
    return KtorResponseMapper().mapResponse(clientResp.call.response)
  }


  override fun close() {
    client.close()
  }

}
