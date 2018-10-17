package net.devslash

import org.apache.http.client.utils.URLEncodedUtils
import org.apache.http.message.BasicNameValuePair
import java.nio.charset.Charset


private class BasicBodyProvider(private val body: String, val data: RequestData) : BodyProvider {
  override fun get(): String {
    var copy = "" + body
    data.getReplacements().forEach { key, value -> copy = copy.replace(key, value) }
    return copy
  }
}

private class MapBodyProvider(private val body: List<Pair<String, Any?>>,
                              private val data: RequestData) : BodyProvider {
  override fun get(): String {
    val d = data.getReplacements()
    val other = body.map {
      if (it.second is String) {
        val o = it.second as String
        d.getOrElse(it.first) { it.first } to d.getOrElse(o) { o }
      } else {
        d.getOrElse(it.first) { it.first } to it.second
      }
    }.toMap()
    return URLEncodedUtils.format(other.map { BasicNameValuePair(it.key, it.value as String) },
        Charset.defaultCharset())
  }
}

fun getBodyProvider(call: Call, data: RequestData): BodyProvider {
  if (call.body == null) {
    return BasicBodyProvider("", data)
  }

  if (call.body!!.bodyParams != null) {
    return MapBodyProvider(call.body!!.bodyParams!!, data)
  }

  if (call.body!!.value != null) {
    return BasicBodyProvider(call.body!!.value!!, data)
  }

  return BasicBodyProvider("", data)
}
