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

class SingleUseDataSupplier<T>(supply: List<String> = listOf()) : RequestDataSupplier<T> {
  private val unsafeSupply: T = supply as T
  private val first = AtomicBoolean(true)

  override suspend fun getDataForRequest(): RequestData<T>? {
    if (first.compareAndSet(true, false)) {
      return object : RequestData<T>() {
        override fun <T> visit(visitor: RequestVisitor<T, Any?>): T {
          return visitor(unsafeSupply, List::class.java)
        }

        override fun get(): T = unsafeSupply
      }
    }
    return null
  }
}
