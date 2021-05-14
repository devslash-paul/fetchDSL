package net.devslash.data

import kotlinx.coroutines.delay
import net.devslash.HttpResponse
import net.devslash.RequestData
import net.devslash.RequestDataSupplier
import net.devslash.SimpleAfterHook
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * An after hook is necessary for this hook - as it's important to track if all requests have been
 * attempted. This is due to the fact that the *very* last request may add more data, and thus when
 * the [delegate] sends its last [RequestData] we must wait till that data has run through the
 * after hooks before we know that there's no more [RequestData] to add to the [modifiedQueue].
 *
 * During this time between the last from the delegate, and the final request going through the
 * [getDataForRequest] method will block the calling thread as it's unsure of its response.
 */
class ModifiableSupplier<T>(private val delegate: RequestDataSupplier<T>) : RequestDataSupplier<T>, SimpleAfterHook {

  private val modifiedQueue: ConcurrentLinkedQueue<RequestData> = ConcurrentLinkedQueue()

  private val sentRequests = AtomicInteger(0)
  private val receivedResponses = AtomicInteger(0)

  fun add(data: RequestData) = modifiedQueue.add(data)

  override suspend fun getDataForRequest(): RequestData? {
    val data = delegate.getDataForRequest()
    if (data != null) {
      // we'll send a request, thus count.
      sentRequests.incrementAndGet()
      return data
    }

    val added = modifiedQueue.poll()
    if (added != null) {
      sentRequests.incrementAndGet()
      return added
    }
    return waitOrNull()
  }

  /**
   * In this case, there was no obvious candidate. Thus we want to wait until a request comes
   */
  private suspend fun waitOrNull(): RequestData? {
    while (sentRequests.get() > receivedResponses.get()) {
      // Attempt to find one, if you don't get it then block until there's some output returned
      val result = modifiedQueue.poll()
      if (result != null) {
        return result
      }

      delay(20)
    }
    // last chance
    return modifiedQueue.poll()
  }

  override fun accept(resp: HttpResponse) {
    receivedResponses.incrementAndGet()
  }
}
