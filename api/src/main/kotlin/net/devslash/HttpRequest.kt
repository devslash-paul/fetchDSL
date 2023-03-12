package net.devslash

import java.io.File
import java.net.URI
import java.time.Duration
import java.time.Instant
import java.util.*

class HttpRequest(val type: HttpMethod, val url: String, val body: Body) {
  val headers: TreeMap<String, MutableList<String>> =
      TreeMap(String.CASE_INSENSITIVE_ORDER)

  fun addHeader(name: String, value: String) {
    headers.putIfAbsent(name, mutableListOf())
    headers[name]!!.add(value)
  }
}

data class HttpResponse(
    var uri: URI,
    val statusCode: Int,
    val headers: Map<String, List<String>>,
    @Suppress("ArrayInDataClass")
    var body: ByteArray,
    val requestStartTime: Instant = Instant.now(),
    val responseStartTime: Instant = Instant.now(),
    val requestDuration: Duration = Duration.ZERO,
)
