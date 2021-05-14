package net.devslash

import java.net.URI
import java.util.*

class HttpRequest(val type: HttpMethod, val url: String, val body: BodyProvider) {
  val headers = TreeMap<String, MutableList<String>>(String.CASE_INSENSITIVE_ORDER)

  fun addHeader(name: String, value: String) {
    if (headers[name] == null) {
      headers[name] = mutableListOf()
    }

    headers[name]!!.add(value)
  }
}

data class HttpResponse(
  var url: URI,
  val statusCode: Int,
  val headers: Map<String, List<String>>,
  var body: ByteArray
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as HttpResponse

    if (url != other.url) return false
    if (statusCode != other.statusCode) return false
    if (headers != other.headers) return false
    if (!body.contentEquals(other.body)) return false

    return true
  }

  override fun hashCode(): Int {
    var result = url.hashCode()
    result = 31 * result + statusCode
    result = 31 * result + headers.hashCode()
    result = 31 * result + body.contentHashCode()
    return result
  }
}

