package net.devslash

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlin.math.max

/**
 * This rate limiter isn't perfect but aims to ensure the rate isn't going too fast, and that the rate is smooth. This
 * means that calls to acquire will only unblock one at a time. If two concurrent calls are made to acquire, then the
 * first will unblock straight away. The second one will unblock after `1 / QPS` seconds.
 *
 * This means that the rate limiter effectively has no memory of the past, beyond the call before. This means that if
 * there is inconsistency with how fast requests are performed, then the rate limit may be over-restrictive.
 */
class AcquiringRateLimiter(private val rateLimitOptions: RateLimitOptions, private val clock: Clock = Clock.systemUTC()) {
  private var lastRelease = Instant.ofEpochMilli(0)
  // This equals how many milliseconds it takes to release a ticket
  private val qps = rateLimitOptions.duration.toMillis() / max(1, rateLimitOptions.count)
  private val lock = Mutex()

  suspend fun acquire() {
    if (!rateLimitOptions.enabled) {
      return
    }

    lock.lock()
    val delayMs = getTimeOfNextAcquire()
    delay(delayMs)
    lock.unlock()
  }

  fun tryAcquire(): Boolean {
    if (!rateLimitOptions.enabled) {
      return true
    }

    return if (lock.tryLock()) {
      val result = getTimeOfNextAcquire()
      lock.unlock()
      result <= 0
    } else {
      false
    }
  }

  /**
   * This function attempts to figure out when the next update can occur. This
   */
  private fun getTimeOfNextAcquire(): Long {
    val now = clock.instant()
    val diff = Duration.between(lastRelease, now).toMillis()
    // We assume this ticket gets consumed, this lets update out last release
    lastRelease = lastRelease.plusMillis(diff)
    return (qps - diff).coerceAtLeast(0L)
  }
}
