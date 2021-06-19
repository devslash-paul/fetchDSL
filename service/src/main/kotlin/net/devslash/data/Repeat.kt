package net.devslash.data

import net.devslash.RequestData
import net.devslash.RequestDataSupplier
import net.devslash.RequestVisitor
import java.util.concurrent.atomic.AtomicInteger

class Repeat(private val repeat: Int) : RequestDataSupplier<Unit> {

  init {
    check(repeat > 0) {
      "Repeat cannot be less than 1"
    }
  }

  private val unitData: RequestData = object : RequestData {
    override fun <T> visit(visitor: RequestVisitor<T, Any?>): T = visitor(Unit, Unit::class.java)
  }
  private val count = AtomicInteger(0)

  override suspend fun getDataForRequest(): RequestData? {
    val current = count.incrementAndGet()
    if (current > repeat) {
      return null
    }
    return unitData
  }
}
