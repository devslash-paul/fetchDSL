package net.devslash.pre

import net.devslash.*
import java.util.concurrent.atomic.AtomicBoolean

class Once(private val before: BeforeHook) : SessionPersistingBeforeHook {

  private val flag = AtomicBoolean(false)

  override suspend fun accept(
    sessionManager: SessionManager,
    cookieJar: CookieJar,
    req: HttpRequest,
    data: RequestData
  ) {
    if (flag.compareAndSet(false, true)) {
      when (before) {
        is SessionPersistingBeforeHook -> before.accept(sessionManager, cookieJar, req, data)
        is SimpleBeforeHook -> before.accept(req, data)
        is SkipBeforeHook -> before.skip(data)
      }
    }
  }
}
