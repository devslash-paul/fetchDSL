package net.devslash

import java.io.InputStream

// TODO: This feels impl based. Create API abstraction for this?
sealed class BodyProvider

typealias Form = Map<String, List<String>>

class RawBody(val raw: InputStream): BodyProvider()

class BasicBodyProvider(
    private val body: String,
    private val data: RequestData<*>,
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
  // Early return, as an empty form can otherwise automatically
  // Fail out due to the mustGet default
  if (form.isEmpty()) {
    form
  } else {
    val indexes = reqData.mustGet<List<String>>().mapIndexed { index, string ->
      "!" + (index + 1) + "!" to string
    }.toMap()
    form.map { (formKey, formValue) ->
      val key = replaceString(indexes, formKey)
      val value = formValue.map { replaceString(indexes, it) }
      return@map key to value
    }.toMap()
  }
}

class MultipartForm(val parts: List<FormPart>) : BodyProvider()

class FormBody(
    private val body: Map<String, List<String>>,
    private val data: RequestData<*>,
    private val mapper: ValueMapper<Map<String, List<String>>>
) : BodyProvider() {
  fun get(): Map<String, List<String>> = mapper(body, data)
}

fun getBodyProvider(call: Call<*>, data: RequestData<*>): BodyProvider {
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

  if (call.body.rawValue != null) {
    return RawBody(call.body.rawValue!!(data))
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
