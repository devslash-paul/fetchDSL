package net.devslash


class BasicBodyProvider(
  private val body: String,
  private val data: RequestData,
  private val mapper: ValueMapper<String>
) : BodyProvider {
  fun get(): String {
    return mapper(body, data)
  }
}

class JsonBody(private val any: Any) : BodyProvider {
  fun get(): Any {
    return any
  }
}

fun getBodyProvider(call: Call<*>, data: RequestData): BodyProvider {
  if (call.body == null) {
    return EmptyBodyProvider
  }

  if (call.body.jsonObject !== null) {
    return JsonBody(call.body.jsonObject)
  }

  if (call.body.lazyJsonObject != null) {
    val lazyJsonObject = call.body.lazyJsonObject
    return JsonBody(lazyJsonObject(data))
  }

  if (call.body.formData != null) {
    return FormBody(call.body.formData, data, call.body.formMapper!!)
  }

  if (call.body.bodyValue != null) {
    return BasicBodyProvider(call.body.bodyValue, data, call.body.bodyValueMapper!!)
  }

  if(call.body.multipartForm != null) {
    return MultipartForm(call.body.multipartForm)
  }

  return EmptyBodyProvider
}

object EmptyBodyProvider : BodyProvider
