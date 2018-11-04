package net.devslash

import java.net.URL

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

fun getResponseWithBody(body: ByteArray) : HttpResponse {
  return HttpResponse(URL("http://example.com"), 200, mapOf(), body)
}

fun getCall(sup: HttpBody? = null, url: String = "http://google.com") = CallBuilder(url).apply {
  body = sup
}.build()

