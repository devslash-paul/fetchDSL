package net.devslash

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class FakeClock(private var instant: Instant) : Clock() {
  override fun withZone(zone: ZoneId?): Clock {
    return this
  }

  override fun getZone(): ZoneId {
    return ZoneId.systemDefault()
  }

  override fun instant(): Instant {
    return instant
  }

  fun advance(d: Duration) {
    instant = instant.plus(d)
  }
}
