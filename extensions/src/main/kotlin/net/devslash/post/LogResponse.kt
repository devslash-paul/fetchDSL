package net.devslash.post

import net.devslash.HttpResponse
import net.devslash.SimplePostHook

class LogResponse : SimplePostHook {
  override fun accept(resp: HttpResponse) {
    println("${resp.url} -> ${resp.statusCode}")
  }
}
