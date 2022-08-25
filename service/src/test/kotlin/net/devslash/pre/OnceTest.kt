package net.devslash.pre

import kotlinx.coroutines.runBlocking
import net.devslash.*
import net.devslash.util.*
import org.junit.Assert.assertEquals
import org.junit.Test

internal class OnceTest {

  @Test
  fun testLambda() = runBlocking {
    var count = 0
    val o = Once({ count++; Unit }.toPreHook())

    o.accept(getUntypedCallRunner(), getCookieJar(), basicRequest(), requestDataFromList(listOf()))

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

    o.accept(getUntypedCallRunner(), getCookieJar(), basicRequest(), requestDataFromList(listOf()))
    o.accept(getUntypedCallRunner(), getCookieJar(), basicRequest(), requestDataFromList(listOf()))

    assertEquals(1, count)
  }

  @Test
  fun testWorksWithComplexHook() = runBlocking {
    var count = 0
    val o = Once(object : SessionPersistingBeforeHook {
      override suspend fun accept(
          subCallRunner: CallRunner<*>,
          cookieJar: CookieJar,
          req: HttpRequest,
          data: RequestData<*>
      ) {
        count++
      }
    })

    o.accept(getUntypedCallRunner(), getCookieJar(), basicRequest(), requestDataFromList(listOf()))

    assertEquals(1, count)
  }

}
