package net.devslash.runner.pre

import net.devslash.SimplePreHook
import net.devslash.runner.HttpRequest
import net.devslash.runner.RequestData

class LogRequest : SimplePreHook {
  override suspend fun accept(req: HttpRequest, data: RequestData) {
    println("Requesting to ${req.url}")
  }
}
