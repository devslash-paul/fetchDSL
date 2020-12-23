package net.devslash

import java.util.concurrent.atomic.AtomicBoolean

internal interface URLProvider {
  fun get(): String
}

internal fun handleNoSupplier(data: RequestDataSupplier<*>?): RequestDataSupplier<*> {
  if (data != null) {
    data.init()
    return data
  }

  // The default re
  return SingleUseDataSupplier()
}

public class SingleUseDataSupplier(private val supply: Map<String, String> = mapOf()) : RequestDataSupplier<Map<String, String>> {
  private val first = AtomicBoolean(true)

  override suspend fun getDataForRequest(): RequestData<Map<String, String>>? {
    if (first.compareAndSet(true, false)) {
      return object : RequestData<Map<String, String>> {
        override fun getReplacements(): Map<String, String> {
          return supply
        }

        override fun get(): Map<String, String> {
          return supply
        }

        override fun accept(v: String): String {
          supply.forEach { (key, value) ->
            v.replace(key, value)
          }
          return v
        }
      }
    }
    return null
  }
}
