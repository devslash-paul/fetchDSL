package net.devslash.pre

import net.devslash.*
import java.util.concurrent.atomic.AtomicBoolean

class Once(private val pre: PreHook) : SessionPersistingPreHook {
  private val flag = AtomicBoolean(false)

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
