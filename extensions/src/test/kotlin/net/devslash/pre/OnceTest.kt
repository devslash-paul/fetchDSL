package net.devslash.pre

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.devslash.*
import net.devslash.util.getBasicRequest
import net.devslash.util.getCookieJar
import net.devslash.util.getSessionManager
import net.devslash.util.requestDataFromList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

internal class OnceTest {

  @Test
  fun testOnlySingle() = runBlocking {
    var count = 0
    val o = Once(object : BeforeHook {
      fun invoke(sess: SessionManager) {
        count += 1
      }
    })

    o.accept(getSessionManager(), getCookieJar(), getBasicRequest(), requestDataFromList(listOf()))
    o.accept(getSessionManager(), getCookieJar(), getBasicRequest(), requestDataFromList(listOf()))

    assertEquals(1, count)
  }

  @Test
  fun testFailsWithInvalidHook() {
    runBlocking {
      val o = Once(object : BeforeHook {
        // invalid as String is not available for injection
        fun invoke(s: String) {
        }
      })

      try {
        o.accept(getSessionManager(),
            getCookieJar(),
            getBasicRequest(),
            requestDataFromList(listOf()))
        fail("Should have an exception")
      } catch (e: InvalidHookException) {
        // ignore
      }
    }
  }

  @Test
  fun testWorksWithComplexHook() = runBlocking {
    var count = 0
    var o = Once(object: SessionPersistingBeforeHook {
      override suspend fun accept(sessionManager: SessionManager,
                                  cookieJar: CookieJar,
                                  req: HttpRequest,
                                  data: RequestData) {
        count++
      }
    })

    o.accept(getSessionManager(), getCookieJar(), getBasicRequest(), requestDataFromList(listOf()))

    assertEquals(1, count)
  }

}
