package net.devslash

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import net.devslash.decorators.PollUntil
import net.devslash.util.basicRequest
import net.devslash.util.basicResponse
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.fail
import org.junit.Test

internal class PollUntilTest {

  @Test
  fun `test poll until base case`() {
    runBlocking {
      var complete = false
      val poller = PollUntil({ complete }, Unit)
      withTimeout(100) {
        val data = poller.getDataForRequest()
        assertThat(data, not(nullValue()))
        assertThat(data!!.get(), equalTo(Unit))
      }
      // This should then refuse to return
      try {
        withTimeout(250) {
          poller.getDataForRequest()
        }
        fail("Timeout must occur")
      } catch (e: TimeoutCancellationException) {
        // pass
      }
      // Should pass when data passed back
      poller.accept(basicRequest(), basicResponse(), Unit)
      withTimeout(100) {
        val data = poller.getDataForRequest()
        assertThat(data, not(nullValue()))
        assertThat(data!!.get(), equalTo(Unit))
      }
      // Once complete. should return null
      complete = true
      poller.accept(basicRequest(), basicResponse(), Unit)
      withTimeout(100) {
        val data = poller.getDataForRequest()
        assertThat(data, nullValue())
      }
    }
  }
}
