package net.devslash.pipes

import net.devslash.*
import java.util.concurrent.ConcurrentLinkedDeque

class Pipe<T>(
  val acceptor: (HttpResponse, RequestData) -> List<RequestData>
) :
  FullDataAfterHook, RequestDataSupplier<T> {

  private val storage = ConcurrentLinkedDeque<RequestData>()

  override suspend fun getDataForRequest(): RequestData? {
    return storage.poll() ?: return null
  }

  override fun accept(req: HttpRequest, resp: HttpResponse, data: RequestData) {
    val newResults = acceptor(resp, data)
    storage.addAll(newResults)
  }
}
