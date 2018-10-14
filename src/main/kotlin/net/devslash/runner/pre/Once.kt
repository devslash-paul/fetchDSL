package net.devslash.runner.pre

import net.devslash.PreHook
import net.devslash.SessionPersistingPreHook
import net.devslash.SimplePreHook
import net.devslash.runner.CookieJar
import net.devslash.runner.HttpRequest
import net.devslash.runner.RequestData
import net.devslash.runner.SessionManager
import java.util.concurrent.atomic.AtomicBoolean

class Once(val pre: PreHook) : SessionPersistingPreHook {
  val flag = AtomicBoolean(false)

  override suspend fun accept(sessionManager: SessionManager,
                              cookieJar: CookieJar,
                              req: HttpRequest,
                              data: RequestData) {
    if (flag.compareAndSet(false, true)) {
      when (pre) {
        is SimplePreHook -> pre.accept(req, data)
        is SessionPersistingPreHook -> pre.accept(sessionManager, cookieJar, req, data)
      }
    }
  }

}
