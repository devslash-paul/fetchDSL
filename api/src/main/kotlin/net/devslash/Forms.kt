package net.devslash

typealias Form = Map<String, List<String>>

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

class FormBody(
  private val body: Map<String, List<String>>,
  private val data: RequestData,
  private val mapper: ValueMapper<Map<String, List<String>>>
) : BodyProvider {
  fun get(): Map<String, List<String>> = mapper(body, data)
}