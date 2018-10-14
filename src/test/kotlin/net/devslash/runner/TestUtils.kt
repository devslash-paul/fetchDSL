package net.devslash.runner

import net.devslash.Call
import net.devslash.HttpBody
import net.devslash.HttpMethod

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

//fun getCall(sup: HttpBody? = null, url: String = "http://google.com") = Call(url, null, null, null,
//    null, null, HttpMethod.GET, null, sup, false, listOf(), preProcessors)
