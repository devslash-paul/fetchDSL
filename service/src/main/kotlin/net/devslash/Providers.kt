package net.devslash

import java.util.concurrent.atomic.AtomicBoolean

interface URLProvider {
  fun get(): String
}

fun handleNoSupplier(data: RequestDataSupplier?): RequestDataSupplier {
  if (data != null) {
    data.init()
    return data as RequestDataSupplier
  }

  // The default re
  return SingleUseDataSupplier()
}

class SingleUseDataSupplier(val supply: List<String> = listOf()) : RequestDataSupplier {
  private val first = AtomicBoolean(true)

  override suspend fun getDataForRequest(): RequestData? {
    if (first.compareAndSet(true, false)) {
      return object : RequestData {
        override fun <T> visit(visitor: RequestVisitor<T, Any?>): T {
          return visitor(supply, List::class.java)
        }
      }
    }
    return null
  }
}
