package net.devslash

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

@ExperimentalCoroutinesApi
class AcquiringRateLimiterTest {
  private lateinit var onePerSecond: AcquiringRateLimiter
  private lateinit var clock: FakeClock

  @BeforeEach
  fun setup() {
    onePerSecond = AcquiringRateLimiter(RateLimitOptions(true, 1, Duration.of(1, ChronoUnit.SECONDS)))
    clock = FakeClock(Instant.ofEpochMilli(0))

  }

  @Test
  fun `Second request within rate does not fire`() = runBlockingTest {
    onePerSecond.acquire()
    val res = async {
      runCatching {
        withTimeout(100) {
          onePerSecond.acquire()
        }
      }
    }
    assertEquals(res.await().isFailure, true)

    clock.advance(Duration.ofSeconds(2))
    onePerSecond.acquire()
    println("God here")
  }
}
