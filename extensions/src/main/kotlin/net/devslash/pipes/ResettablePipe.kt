package net.devslash.pipes

import net.devslash.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * Must like the [Pipe] except this can be reset via a pre hook. A good use of this is to
 * have a call that first populates the pipe. Then in the following calls, a [net.devslash.pre.Once]
 * should be used to reset the pipe.
 */
class ResettablePipe(val acceptor: (HttpResponse, RequestData) -> List<String>, val split: String? = null) : BasicOutput, RequestDataSupplier {

  private val index = AtomicInteger(0)
  private val storage = mutableListOf<String>()

  override fun getDataForRequest(): RequestData {
    val currentValue = storage[index.getAndIncrement()]
    val line = if (split != null) {
      currentValue.split(split)
    } else listOf(currentValue)

    return ListBasedRequestData(line)
  }

  override fun hasNext(): Boolean = index.get() < storage.size


  override fun accept(resp: HttpResponse, data: RequestData) {
    val newResults = acceptor(resp, data)
    storage.addAll(newResults)
  }

  fun reset() {
    index.set(0)
  }
}
