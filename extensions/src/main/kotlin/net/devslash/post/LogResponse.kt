package net.devslash.post

import net.devslash.HttpResponse
import net.devslash.SimpleAfterHook

class LogResponse : SimpleAfterHook {
  override fun accept(resp: HttpResponse) {
    if (resp.statusCode < 200) {
      println("error: " + String(resp.body))
    }
    println("Resp ${resp.url} -> ${resp.statusCode}")
  }
}
