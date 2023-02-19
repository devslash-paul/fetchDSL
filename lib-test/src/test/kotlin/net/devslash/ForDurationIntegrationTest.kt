package net.devslash

import net.devslash.data.ForDuration
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.lessThan
import org.junit.ClassRule
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentSkipListSet
import kotlin.math.abs

class ForDurationIntegrationTest {
  companion object {
    @JvmField
    @ClassRule
    val testServer = TestServer()
  }

  @Test(timeout = 15000)
  fun testSimpleDurationSucceeds() {
    // warmup
    runHttp {
      call("${testServer.address()}/bounce")
    }
    val startTimes = ConcurrentSkipListSet<Instant>()
    runHttp {
      call<Unit>("${testServer.address()}/bounce") {
        rateLimit(25, Duration.ofSeconds(1))
        install(ForDuration(Duration.ofSeconds(2)) { })

        after {
          action {
            startTimes.add(resp.requestStartTime)
          }
        }
      }
    }
    val started = startTimes.minOf { it.toEpochMilli() }
    val ended = startTimes.maxOf { it.toEpochMilli() }
    assertThat(abs(ended - started - 2000), lessThan(100))
  }

}
