package net.devslash.pre

import net.devslash.HttpRequest
import net.devslash.RequestData
import net.devslash.SimplePreHook

class LogRequest : SimplePreHook {
  override fun accept(req: HttpRequest, data: RequestData) {
    println("Requesting to ${req.url}")
  }
}
