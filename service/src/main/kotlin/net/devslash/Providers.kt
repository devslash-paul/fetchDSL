package net.devslash

import java.util.concurrent.atomic.AtomicBoolean


fun <T> handleNoSupplier(data: RequestDataSupplier<T>?): RequestDataSupplier<T> {
  if (data != null) {
    data.init()
    return data
  }

  // The default re
  return SingleUseDataSupplier()
}

class SingleUseDataSupplier<T>(val supply: List<String> = listOf()) : RequestDataSupplier<T> {
  private val first = AtomicBoolean(true)

  override suspend fun getDataForRequest(): RequestData? {
    if (first.compareAndSet(true, false)) {
      return object : RequestData() {
        override fun <T> visit(visitor: RequestVisitor<T, Any?>): T {
          return visitor(supply, List::class.java)
        }
      }
    }
    return null
  }
}
