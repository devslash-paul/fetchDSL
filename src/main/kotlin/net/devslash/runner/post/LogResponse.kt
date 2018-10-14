package net.devslash.runner.post

import net.devslash.SimplePostHook
import net.devslash.runner.HttpResponse

class LogResponse : SimplePostHook {
  override fun accept(resp: HttpResponse) {
    println("${resp.url} -> ${resp.statusCode}")
  }

}
