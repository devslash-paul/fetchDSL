package net.devslash.pipes

import net.devslash.*
import java.util.concurrent.atomic.AtomicInteger

class ResettablePipe<T>(
  val acceptor: (HttpResponse, RequestData<T>) -> List<T>
) : BasicOutput, RequestDataSupplier<List<T>> {

  private val index = AtomicInteger(0)
  private val storage = mutableListOf<T>()

  override suspend fun getDataForRequest(): RequestData<List<T>>? {
    val currentValue = storage.getOrNull(index.getAndIncrement()) ?: return null
    return ListBasedRequestData(listOf(currentValue))
  }

  override fun init() {
    reset()
  }

  override fun <K> accept(req: HttpRequest, resp: HttpResponse, data: RequestData<K>) {
    val newResults = acceptor(resp, data as RequestData<T>)
    storage.addAll(newResults)
  }

  fun reset() {
    index.set(0)
  }
}
