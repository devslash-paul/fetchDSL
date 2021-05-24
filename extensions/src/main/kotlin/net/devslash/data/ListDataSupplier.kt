package net.devslash.data

import net.devslash.ListRequestData
import net.devslash.RequestData
import net.devslash.RequestDataSupplier
import java.util.concurrent.atomic.AtomicInteger

class ListDataSupplier<T>(private val list: Lazy<List<T>>, private val clazz: Class<T>) : RequestDataSupplier<T> {
  private val line = AtomicInteger(0)

  companion object {
    inline operator fun <reified T> invoke(list: List<T>): ListDataSupplier<T> {
      return ListDataSupplier(lazy { list }, T::class.java)
    }

    inline operator fun <reified T> invoke(list: Lazy<List<T>>): ListDataSupplier<T> {
      return ListDataSupplier(list, T::class.java)
    }

    inline fun <reified T> single(item: T): ListDataSupplier<T> {
      return ListDataSupplier(lazy { listOf(item) }, T::class.java)
    }
  }

  override suspend fun getDataForRequest(): RequestData? {
    val index = line.getAndIncrement()
    val obj = list.value.getOrNull(index) ?: return null
    return ListRequestData(obj, clazz)
  }
}
