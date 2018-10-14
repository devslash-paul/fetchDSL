package net.devslash.runner.pre

import kotlinx.coroutines.joinAll
import net.devslash.SessionPersistingPreHook
import net.devslash.runner.canva.canvaProdCsrf
import net.devslash.runner.canva.canvaProdLogin
import net.devslash.runner.*
import java.util.function.Consumer

class CanvaProdLogin(private val fromFile: String) : SessionPersistingPreHook {
  // Only authenticate once
  var authenticated = false

  override suspend fun accept(sessionManager: SessionManager,
                              cookieJar: CookieJar,
                              req: HttpRequest,
                              data: RequestData) {
    if (!authenticated) {
      var awaitingCsrf = ""
      sessionManager.call(
          canvaProdCsrf(Consumer { it -> awaitingCsrf = it })).joinAll()
      sessionManager.call(canvaProdLogin(awaitingCsrf, fromFile), cookieJar).joinAll()
      authenticated = true
    }
  }
}
