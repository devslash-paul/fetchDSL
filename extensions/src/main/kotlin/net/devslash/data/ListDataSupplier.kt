package net.devslash.data

import net.devslash.GenericRequestData
import net.devslash.RequestData
import net.devslash.RequestDataSupplier
import java.util.concurrent.atomic.AtomicInteger

// This aims to create multiple request data pieces. Therefore out can be any type
class ListDataSupplier<Out, InType, L : List<InType>> : RequestDataSupplier<Out> {
  private val line = AtomicInteger(0)
  private val list: Lazy<L>
  private val transform: (InType) -> Out

  companion object {
    @JvmName("invoke1")
    operator fun invoke(list: List<String>): ListDataSupplier<List<String>, String, List<String>> {
      return ListDataSupplier(list, { listOf(it) })
    }

    operator fun <T> invoke(list: List<T>): ListDataSupplier<T, T, List<T>> {
      return ListDataSupplier(list, {it})
    }
  }

  constructor(
    list: L,
    transform: (InType) -> Out
  ) {
    this.list = lazy { list }
    this.transform = transform
  }

  constructor(
    list: Lazy<L>,
    transform: (InType) -> Out
  ) {
    this.list = list
    this.transform = transform
  }

  override suspend fun getDataForRequest(): RequestData<Out>? {
    val index = line.getAndIncrement()
    val obj = list.value.getOrNull(index) ?: return null
    return GenericRequestData(transform(obj))
  }
}
