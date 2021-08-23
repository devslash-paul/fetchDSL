package net.devslash.pipes

import net.devslash.*
import java.util.concurrent.ConcurrentLinkedDeque

class Pipe<T, Y> private constructor(
  val acceptor: (HttpResponse, T) -> List<RequestData<Y>>
):
  ResolvedFullDataAfterHook<T>, RequestDataSupplier<Y> {
  companion object {
    operator fun <Y> invoke(acceptor: (HttpResponse, List<String>) -> List<RequestData<Y>>): Pipe<List<String>, Y> {
      return Pipe(acceptor)
    }

    @JvmName("invokeTyped")
    operator fun <T, Y> invoke(acceptor: (HttpResponse, T) -> List<RequestData<Y>>): Pipe<T, Y> {
      return Pipe(acceptor)
    }
  }

  private val storage = ConcurrentLinkedDeque<RequestData<Y>>()

  override suspend fun getDataForRequest(): RequestData<Y>? {
    return storage.poll() ?: return null
  }

  override fun accept(req: HttpRequest, resp: HttpResponse, data: T) {
    val newResults = acceptor(resp, data)
    storage.addAll(newResults)
  }
}
