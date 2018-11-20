package net.devslash


class BasicBodyProvider(private val body: String, val data: RequestData) : BodyProvider {
  fun get(): String {
    var copy = "" + body
    data.getReplacements().forEach { key, value -> copy = copy.replace(key, value) }
    return copy
  }
}

class MapBodyProvider(
  private val body: List<Pair<String, String>>, private val data: RequestData
) : BodyProvider {
  fun get(): Map<String, String> {
    return body.map {
      it.first to it.second.asReplaceableValue().get(data)
    }.toMap()
  }
}

fun getBodyProvider(call: Call, data: RequestData): BodyProvider {
  if (call.body == null) {
    return EmptyBodyProvider()
  }

  if (call.body.bodyParams != null) {
    return MapBodyProvider(call.body.bodyParams, data)
  }

  if (call.body.value != null) {
    return BasicBodyProvider(call.body.value, data)
  }

  return EmptyBodyProvider()
}

class EmptyBodyProvider : BodyProvider
