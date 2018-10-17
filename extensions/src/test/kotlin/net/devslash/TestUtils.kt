package net.devslash

fun requestDataFromList(listOf: List<String>? = null): RequestData {
  return object : RequestData {
    override fun getReplacements(): Map<String, String> {
      if (listOf != null) {
        return listOf.mapIndexed { i, p ->
          "!${i + 1}!" to p
        }.toMap()
      }

      return mapOf()
    }
  }
}

fun getCall(sup: HttpBody? = null, url: String = "http://google.com") = CallBuilder(url).apply {
  body = sup
}.build()
