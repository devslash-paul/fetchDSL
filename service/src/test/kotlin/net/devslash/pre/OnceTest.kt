package net.devslash.pre

import kotlinx.coroutines.runBlocking
import net.devslash.*
import net.devslash.util.basicRequest
import net.devslash.util.getCookieJar
import net.devslash.util.getSessionManager
import net.devslash.util.requestDataFromList
import org.junit.Assert.assertEquals
import org.junit.Test

internal class OnceTest {

  @Test
  fun testLambda() = runBlocking {
    var count = 0
    val o = Once({ count++; Unit }.toPreHook())

    o.accept(getSessionManager(), getCookieJar(), basicRequest(), requestDataFromList(listOf()))

    assertEquals(1, count)
  }

  @Test
  fun testSingleFiresOnce() = runBlocking {
    var count = 0
    val o = Once(object : SimpleBeforeHook {
      override fun accept(req: HttpRequest, data: RequestData<*>) {
        count += 1
      }
    })

    o.accept(getSessionManager(), getCookieJar(), basicRequest(), requestDataFromList(listOf()))
    o.accept(getSessionManager(), getCookieJar(), basicRequest(), requestDataFromList(listOf()))

    assertEquals(1, count)
  }

  @Test
  fun testWorksWithComplexHook() = runBlocking {
    var count = 0
    val o = Once(object : SessionPersistingBeforeHook {
      override suspend fun accept(
        sessionManager: SessionManager,
        cookieJar: CookieJar,
        req: HttpRequest,
        data: RequestData<*>
      ) {
        count++
      }
    })

    o.accept(getSessionManager(), getCookieJar(), basicRequest(), requestDataFromList(listOf()))

    assertEquals(1, count)
  }

}
