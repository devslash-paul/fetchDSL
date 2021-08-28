package net.devslash

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.withTimeout
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.number.OrderingComparison
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.system.measureTimeMillis

@ExperimentalCoroutinesApi
class AcquiringRateLimiterTest {
  private lateinit var tenPerSecond: AcquiringRateLimiter
  private lateinit var timedTicket: TimedTicket
  private lateinit var clock: FakeClock

  @Before
  fun setup() {
    clock = FakeClock(Instant.ofEpochMilli(100000))
    tenPerSecond = AcquiringRateLimiter(RateLimitOptions(true, 10, Duration.of(1, ChronoUnit.SECONDS)), clock)
    // 100ms per ticket
    timedTicket = TimedTicket(100L, clock)
  }

  @Test
  fun `Test two tickets require delay`() = runBlocking {
    val taken = measureTimeMillis {
      tenPerSecond.acquire()
      tenPerSecond.acquire()
    }
    assertThat(taken, OrderingComparison.greaterThan(99L))
  }

  @Test
  fun `After request time step suggested for waiting is correct`() {
    timedTicket.setLastRelease()
    assertThat(timedTicket.timeTillNextRelease(), equalTo(100))
    clock.advance(Duration.ofMillis(45))
    assertThat(timedTicket.timeTillNextRelease(), equalTo(55))
    clock.advance(Duration.ofMillis(55))
    assertThat(timedTicket.timeTillNextRelease(), equalTo(0))
    clock.advance(Duration.ofMillis(50))
    assertThat(timedTicket.timeTillNextRelease(), equalTo(0))
  }

  @Test
  fun `Attempt request with timeout`() = runBlocking {
    // Give a little leeway
    withTimeout(1100) {
      val time = measureTimeMillis {
        repeat(10) {
          tenPerSecond.acquire()
        }
      }

      assertTrue("Must take more than 900ms due to smoothing", time > 900)
    }
  }

  @Test
  fun `Test consistent gating time when returning partially through wait`() {
    timedTicket.setLastRelease()
    clock.advance(Duration.ofMillis(30))
    assertThat(timedTicket.timeTillNextRelease(), equalTo(70))
    clock.advance(Duration.ofMillis(10))
    timedTicket.setLastRelease()
    assertThat(timedTicket.timeTillNextRelease(), equalTo(100))
  }
}
