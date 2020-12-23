package net.devslash.pre

import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import net.devslash.*
import net.devslash.util.getBasicRequest
import net.devslash.util.getCookieJar
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

internal class OnceTest {

  private val sManager = mockk<SessionManager>()

  @Test
  fun testLambda() = runBlocking {
    var count = 0
    val o = Once({ count++; Unit }.toPreHook())

    o.accept(sManager, getCookieJar(), getBasicRequest(), ListBasedRequestData<String>(listOf()))

    assertEquals(1, count)
  }

  @Test
  fun testOnlySingle() = runBlocking {
    var count = 0
    val o = Once(object : BeforeHook {

      @Suppress("unused")
      fun invoke(@Suppress("UNUSED_PARAMETER") sess: SessionManager) {
        count += 1
      }
    })

    o.accept(sManager, getCookieJar(), getBasicRequest(), ListBasedRequestData<String>(listOf()))
    o.accept(sManager, getCookieJar(), getBasicRequest(), ListBasedRequestData<String>(listOf()))

    assertEquals(1, count)
  }

  @Test
  fun testFailsWithInvalidHook() {
    runBlocking {
      val o = Once(object : BeforeHook {
        // invalid as String is not available for injection
        @Suppress("unused")
        fun invoke(@Suppress("UNUSED_PARAMETER") s: String) {
        }
      })

      try {
        o.accept(sManager,
            getCookieJar(),
            getBasicRequest(),
            ListBasedRequestData<String>(listOf()))
        fail("Should have an exception")
      } catch (e: InvalidHookException) {
        // ignore
      }
    }
  }

  @Test
  fun testWorksWithComplexHook() = runBlocking {
    var count = 0
    val o = Once(object : SessionPersistingBeforeHook {
      override suspend fun <T> accept(
        sessionManager: SessionManager,
        cookieJar: CookieJar,
        req: HttpRequest,
        data: RequestData<T>
      ) {
        count++
      }
    })

    o.accept(sManager, getCookieJar(), getBasicRequest(), ListBasedRequestData<String>(listOf()))

    assertEquals(1, count)
  }

}
