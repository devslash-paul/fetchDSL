package net.devslash.pipes

import net.devslash.*
import java.util.concurrent.atomic.AtomicInteger

class ResettablePipe(val acceptor: (HttpResponse, RequestData) -> List<String>,
                     private val split: String? = null) : BasicOutput, RequestDataSupplier {

  private val index = AtomicInteger(0)
  private val storage = mutableListOf<String>()

  override fun getDataForRequest(): RequestData {
    val currentValue = storage[index.getAndIncrement()]
    val line = if (split != null) {
      currentValue.split(split)
    } else listOf(currentValue)

    return ListBasedRequestData(line)
  }

  override fun init() {
    reset()
  }

  override fun hasNext(): Boolean = index.get() < storage.size

  override fun accept(req: HttpRequest, resp: HttpResponse, data: RequestData) {
    val newResults = acceptor(resp, data)
    storage.addAll(newResults)
  }

  fun reset() {
    index.set(0)
  }
}
