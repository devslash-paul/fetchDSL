package net.devslash.data

import net.devslash.ListBasedRequestData
import net.devslash.RequestData
import net.devslash.RequestDataSupplier
import java.util.concurrent.atomic.AtomicInteger

class ListDataSupplier<T> : RequestDataSupplier {
  private val line = AtomicInteger(0)
  private val list: Lazy<List<T>>
  private val transform: (T) -> List<String>

  constructor(list: List<T>,
              transform: (T) -> List<String> = { listOf("$it") }) {
    this.list = lazy { list }
    this.transform = transform

  }

  constructor(list: Lazy<List<T>>,
              transform: (T) -> List<String> = { listOf("$it") }) {
    this.list = list
    this.transform = transform
  }

  override suspend fun getDataForRequest(): RequestData? {
    val index = line.getAndIncrement()
    val obj = list.value.getOrNull(index) ?: return null
    return ListBasedRequestData(transform(obj))
  }
}
