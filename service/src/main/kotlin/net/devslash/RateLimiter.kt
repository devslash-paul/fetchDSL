package net.devslash

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

class AcquiringRateLimiter(private val rateLimitOptions: RateLimitOptions, private val clock: Clock = Clock.systemUTC()) {
  // how many milliseconds between each ticket release
  // So - 100 tickets per second would be 10 ms Release time
  private val msRelease: Long = rateLimitOptions.duration.toMillis() / rateLimitOptions.count
  private val tickets = AtomicInteger(0)
  private val lastUpdate = clock.instant()

  private val lock = Mutex()
  private val awaitingTicket = Semaphore(1)
  // Rolling window - take a number able to be done per 10 seconds and attempt to roll

  suspend fun acquire() {
    if (!rateLimitOptions.enabled) {
      return
    }

    if (lock.tryLock()) {
      updateInternal()
      lock.unlock()
    }
    awaitingTicket.acquire()
  }

  private fun updateInternal() {
    // We only have to do a ticket update when there's an attempt at acquiring.
    val now = clock.instant()
    val diff = Duration.between(lastUpdate, now)
    val steps = diff.toMillis() / msRelease
    // We only advance `lastUpdate` to the amount that was given by the steps. This avoids us going under the rate limit
    // by a small margin by a rounding error
    lastUpdate.plusMillis(steps * msRelease)
    repeat(steps.toInt()) {
      awaitingTicket.release()
    }
  }

  private suspend fun acquireInternal() {
    // We know that maximally at one point there is a single thing attempting to get this
    val current = tickets.getAndDecrement()

    if (current > 0) {
      // If this is the case then we did get a ticket
      return
    }
    // Otherwise we technically didn't receive a ticket. Due to how this works, this means that there are zero tickets
    // thus we have to wait for one to accumulate.
    awaitingTicket.acquire()
  }

}
