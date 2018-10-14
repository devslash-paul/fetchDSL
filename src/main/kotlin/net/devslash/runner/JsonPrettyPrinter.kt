package net.devslash.runner

import com.google.gson.GsonBuilder

class JsonPrettyPrinter : ResponseConsumer {
  private val gson = GsonBuilder().setPrettyPrinting().create()

  override fun accept(resp: HttpResponse) {
    val stringBody = String(resp.body)
    val map = gson.fromJson(stringBody, Map::class.java)
    resp.body = gson.toJson(map).toByteArray()
  }
}
