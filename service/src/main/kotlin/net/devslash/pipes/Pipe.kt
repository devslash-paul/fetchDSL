package net.devslash.pipes

import net.devslash.*
import java.util.concurrent.ConcurrentLinkedDeque

class Pipe<T>(
  val acceptor: (HttpResponse, T) -> List<RequestData<T>>
) :
  ResolvedFullDataAfterHook<T>, RequestDataSupplier<T> {

  private val storage = ConcurrentLinkedDeque<RequestData<T>>()

  override suspend fun getDataForRequest(): RequestData<T>? {
    return storage.poll() ?: return null
  }

  override fun accept(req: HttpRequest, resp: HttpResponse, data: T) {
    val newResults = acceptor(resp, data)
    storage.addAll(newResults)
  }
}
