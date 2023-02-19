package net.devslash

import io.ktor.http.*
import io.ktor.util.*
import io.ktor.util.date.*
import java.net.URI
import java.time.Duration
import java.time.Instant
import kotlin.collections.set

class KtorResponseMapper(private val uriMapper: (it: KtorResponse) -> URI = { URI(it.call.request.url.toString()) }) {

  suspend fun mapResponse(response: io.ktor.client.statement.HttpResponse): HttpResponse {
    return HttpResponse(
      uriMapper(response),
      response.status.value,
      mapHeaders(response.headers),
      response.content.toByteArray(),
      toInstant(response.requestTime),
      toInstant(response.responseTime),
      duration(response.requestTime, response.responseTime),
    )
  }

  private fun toInstant(requestTime: GMTDate): Instant {
    return requestTime.toJvmDate().toInstant()
  }

  private fun duration(requestTime: GMTDate, responseTime: GMTDate): Duration {
    val reqTime = requestTime.toJvmDate().toInstant()
    val respTime = responseTime.toJvmDate().toInstant()
    return Duration.between(reqTime, respTime)
  }

  private fun mapHeaders(headers: Headers): Map<String, List<String>> {
    val map = mutableMapOf<String, List<String>>()
    headers.forEach { key, value -> map[key] = value }
    return map
  }
}
