package net.devslash

import java.util.concurrent.atomic.AtomicBoolean

interface URLProvider {
  fun get(): String
}

fun handleNoSupplier(data: RequestDataSupplier?): RequestDataSupplier {
  if (data != null) {
    data.init()
    return data
  }

  // The default re
  return SingleUseDataSupplier()
}

class SingleUseDataSupplier(private val supply: Map<String, String> = mapOf()) : RequestDataSupplier {
  private val first = AtomicBoolean(true)

  override suspend fun getDataForRequest(): RequestData? {
    if (first.compareAndSet(true, false)) {
      return object : RequestData {
        override fun getReplacements(): Map<String, String> {
          return supply
        }
      }
    }
    return null
  }
}
