package net.devslash.post

import net.devslash.HttpResponse
import net.devslash.SimplePostHook

class LogResponse : SimplePostHook {
  override fun accept(resp: HttpResponse) {
    if (resp.statusCode < 200) {
      println("error: " + String(resp.body))
    }
    println("Resp ${resp.url} -> ${resp.statusCode}")
  }
}
