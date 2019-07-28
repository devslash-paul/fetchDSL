package net.devslash.data

import net.devslash.ListBasedRequestData
import net.devslash.RequestData
import net.devslash.RequestDataSupplier
import java.util.concurrent.atomic.AtomicInteger

class ListDataSupplier<T>(private val list: List<T>,
                          private val transform: (T) -> List<String> = {listOf("$it")}) : RequestDataSupplier {
  private val line = AtomicInteger(0)

  override suspend fun getDataForRequest(): RequestData? {
    val index = line.getAndIncrement()
    val obj = list.getOrNull(index) ?: return null
    return ListBasedRequestData(transform(obj))
  }
}
