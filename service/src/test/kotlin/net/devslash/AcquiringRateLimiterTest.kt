package net.devslash

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.system.measureTimeMillis
import kotlin.time.measureTime

@ExperimentalCoroutinesApi
class AcquiringRateLimiterTest {
  private lateinit var tenPerSecond: AcquiringRateLimiter
  private lateinit var clock: FakeClock

  @BeforeEach
  fun setup() {
    clock = FakeClock(Instant.ofEpochMilli(100000))
    tenPerSecond = AcquiringRateLimiter(RateLimitOptions(true, 10, Duration.of(1, ChronoUnit.SECONDS)), clock)
  }

  @Test
  fun `Second request within rate does not fire`() = runBlockingTest {
    assertTrue(tenPerSecond.tryAcquire())
    assertFalse(tenPerSecond.tryAcquire())
  }

  @Test
  fun `Second request fires after allowed`() = runBlockingTest {
    assertTrue(tenPerSecond.tryAcquire())
    clock.advance(Duration.ofMillis(1001))
    assertTrue(tenPerSecond.tryAcquire())
  }

  @Test
  fun `Attempt request with timeout`() = runBlocking {
    // Give a little leeway
    withTimeout(1200) {
      val time = measureTimeMillis {
        repeat(10) {
          tenPerSecond.acquire()
        }
      }

      assertTrue(time > 900, "Must take more than 900ms due to smoothing")
    }
  }
}
