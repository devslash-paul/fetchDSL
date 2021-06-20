package net.devslash

import java.net.URI
import java.util.*

class HttpRequest(val type: HttpMethod, val url: String, val body: BodyProvider) {
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
  var body: ByteArray
)
