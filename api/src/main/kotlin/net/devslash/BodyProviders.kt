package net.devslash


class BasicBodyProvider(private val body: String, val data: RequestData) : BodyProvider {
  fun get(): String {
    var copy = "" + body
    data.getReplacements().forEach { key, value -> copy = copy.replace(key, value) }
    return copy
  }
}

class FormBody(private val body: Map<String, List<String>>,
               private val data: RequestData) : BodyProvider {
  fun get(): Map<String, List<String>> {
    return body.map {
      val entries = it.value.map { it.asReplaceableValue().get(data) }
      it.key to entries
    }.toMap()
  }
}

class JsonBody(private val any: Any) : BodyProvider {
  fun get(): Any {
    return any
  }
}

fun getBodyProvider(call: Call, data: RequestData): BodyProvider {
  if (call.body == null) {
    return EmptyBodyProvider
  }

  if (call.body.jsonObject !== null) {
    return JsonBody(call.body.jsonObject)
  }

  if (call.body.formData != null) {
    return FormBody(call.body.formData, data)
  }

  if (call.body.value != null) {
    return BasicBodyProvider(call.body.value, data)
  }

  return EmptyBodyProvider
}

object EmptyBodyProvider : BodyProvider
