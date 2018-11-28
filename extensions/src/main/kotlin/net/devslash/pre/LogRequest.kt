package net.devslash.pre

import net.devslash.HttpRequest
import net.devslash.RequestData
import net.devslash.SimpleBeforeHook

class LogRequest : SimpleBeforeHook {
  override fun accept(req: HttpRequest, data: RequestData) {
    println("Requesting to ${req.url}")
  }
}
