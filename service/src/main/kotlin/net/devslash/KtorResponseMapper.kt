package net.devslash

import io.ktor.http.*
import io.ktor.util.*
import java.net.URI
import kotlin.collections.set

class KtorResponseMapper(private val uriMapper: (it: KtorResponse) -> URI = { URI(it.call.request.url.toString()) }) {

  suspend fun mapResponse(response: io.ktor.client.statement.HttpResponse): HttpResponse {
    return HttpResponse(
      uriMapper(response),
      response.status.value,
      mapHeaders(response.headers),
      response.content.toByteArray()
    )
  }

  private fun mapHeaders(headers: Headers): Map<String, List<String>> {
    val map = mutableMapOf<String, List<String>>()
    headers.forEach { key, value -> map[key] = value }
    return map
  }
}