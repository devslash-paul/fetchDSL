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
class AcquiringRateLimiter(
  private val rateLimitOptions: RateLimitOptions,
  clock: Clock = Clock.systemUTC()
) {

  // This equals how many milliseconds it takes to release a ticket
  private val stepMillis = rateLimitOptions.duration.toMillis() / max(1, rateLimitOptions.count)
  private val ticket = TimedTicket(stepMillis, clock)
  private val lock = Mutex()

  suspend fun acquire() {
    if (!rateLimitOptions.enabled) {
      return
    }

    lock.lock()
    val delayMs = ticket.timeTillNextRelease()
    delay(delayMs)
    ticket.setLastRelease()
    lock.unlock()
  }
}

/**
 * This class is split out from the Rate limiter to allow for better testing
 * It effectively should manage the timesteps in which a request ticket can be
 * allowed
 */
class TimedTicket(private val millSteps: Long, private val clock: Clock) {
  private var lastRelease = Instant.ofEpochMilli(0)

  fun timeTillNextRelease(): Long {
    val now = clock.instant()
    val timeSinceLastRelease = Duration.between(lastRelease, now).toMillis()
    return (millSteps - timeSinceLastRelease).coerceAtLeast(0)
  }

  fun setLastRelease() { lastRelease = clock.instant() }
}
