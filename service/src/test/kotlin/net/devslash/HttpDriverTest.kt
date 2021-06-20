package net.devslash

import kotlinx.coroutines.runBlocking
import net.devslash.util.FailingClientAdapter
import net.devslash.util.basicRequest
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

internal class HttpDriverTest {

  @Test
  fun testClientErrorReturned() = runBlocking {
    HttpDriver(FailingClientAdapter()).use {
      val res = it.call(basicRequest())

      assertThat(res, instanceOf(Failure::class.java))
      assertThat((res as Failure).err, instanceOf(RuntimeException::class.java))
    }
  }
}
