package net.devslash

sealed class BodyProvider

typealias Form = Map<String, List<String>>

class BasicBodyProvider(
  private val body: String,
  private val data: RequestData,
  private val mapper: ValueMapper<String>
) : BodyProvider() {
  fun get(): String {
    return mapper(body, data)
  }
}

class JsonBody(private val any: Any) : BodyProvider() {
  fun get(): Any {
    return any
  }
}

val formIdentity: ValueMapper<Map<String, List<String>>> = { form, _ -> form }
val formIndexed: ValueMapper<Map<String, List<String>>> = { form, reqData ->
  val indexes = reqData.mustGet<List<String>>().mapIndexed { index, string ->
    "!" + (index + 1) + "!" to string
  }.toMap()
  form.map { entry ->
    val key = replaceString(indexes, entry.key)
    val value = entry.value.map { replaceString(indexes, it) }
    return@map key to value
  }.toMap()
}

class MultipartForm(val parts: List<FormPart>) : BodyProvider()

class FormBody(
  private val body: Map<String, List<String>>,
  private val data: RequestData,
  private val mapper: ValueMapper<Map<String, List<String>>>
) : BodyProvider() {
  fun get(): Map<String, List<String>> = mapper(body, data)
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

  if (call.body.multipartForm != null) {
    return MultipartForm(call.body.multipartForm)
  }

  if (call.body.lazyMultipartForm != null) {
    val lazyMultipartForm = call.body.lazyMultipartForm
    return MultipartForm(lazyMultipartForm(data))
  }

  return EmptyBodyProvider
}

object EmptyBodyProvider : BodyProvider()
