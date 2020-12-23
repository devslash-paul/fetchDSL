package net.devslash

fun requestDataFromList(listOf: List<String>? = null): RequestData<String> {
  return object : RequestData<String> {
    override fun getReplacements(): Map<String, String> {
      if (listOf != null) {
        return listOf.mapIndexed { i, p ->
          "!${i + 1}!" to p
        }.toMap()
      }

      return mapOf()
    }

    override fun get(): String {
      TODO("Not yet implemented")
    }

    override fun accept(v: String): String {
      val replacements = getReplacements()
      var copy = v
      replacements.forEach { (key, value) -> copy = copy.replace(key, value) }
      return copy
    }
  }
}

fun getBasicRequest(): HttpRequest {
  return HttpRequest(HttpMethod.GET, "https://example.com", EmptyBodyProvider)
}

fun getCookieJar(): CookieJar {
  return CookieJar()
}

internal fun <T> getCall(sup: HttpBody<T>? = null, url: String = "https://example.com") = CallBuilder<T>(
  url
).apply {
  body = sup
}.build()
