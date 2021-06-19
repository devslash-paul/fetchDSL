package net.devslash

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import net.devslash.data.Repeat
import net.devslash.util.basicUrl
import org.hamcrest.core.IsEqual.equalTo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThat
import org.junit.Test
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger

@ExperimentalCoroutinesApi
internal class HttpSessionManagerTest {

  @Test
  fun testBasicHttpBounce() = runBlockingTest {
    val resp = HttpResponse(
      URI(basicUrl),
      200,
      mapOf("set-cookie" to listOf("session=abcd")),
      "Hi there".toByteArray()
    )
    val engine = BounceHttpDriver(resp)
    runHttp(engine) {
      call(basicUrl) {
        after {
          action {
            val cookie = resp.headers["set-cookie"]!![0]
            val body = String(resp.body)
            assertEquals("session=abcd", cookie)
            assertEquals("Hi there", body)
          }
        }
      }
    }
  }

  @Test
  fun testMultiRequest() {
    val testConcurrency = 5
    val repeats = 100
    val atomicInteger = AtomicInteger(0)
    runHttp(BounceHttpDriver()) {
      concurrency = testConcurrency
      call<Unit>(basicUrl) {
        data = Repeat(repeats)
        after {
          action {
            atomicInteger.incrementAndGet()
          }
        }
      }
    }

    assertThat(atomicInteger.get(), equalTo(repeats))
  }
}
