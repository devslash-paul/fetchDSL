package net.devslash

import kotlinx.coroutines.runBlocking
import net.devslash.util.basicRequest
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

internal class HttpDriverTest {

  @Test
  fun testClientErrorReturned() = runBlocking {
    val client = mock<HttpClientAdapter>()
    whenever(client.request(any())).thenThrow(RuntimeException())

    HttpDriver(client).use {
      val res = it.call(basicRequest())

      assertThat(res, instanceOf(Failure::class.java))
      assertThat((res as Failure).err, instanceOf(RuntimeException::class.java))
    }
  }
}
