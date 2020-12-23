package net.devslash.pipes

import net.devslash.*
import java.util.concurrent.ConcurrentLinkedDeque

class Pipe<T>(val acceptor: (HttpResponse, RequestData<T>) -> List<String>, private val split: String? = null) : BasicOutput, RequestDataSupplier<T> {

  private val storage = ConcurrentLinkedDeque<String>()

  override suspend fun getDataForRequest(): RequestData<T>? {
    val currentValue = storage.poll() ?: return null
    val line = if (split != null) {
      currentValue.split(split)
    } else listOf(currentValue)

    return object : RequestData<T> {
      override fun getReplacements(): Map<String, String> {
        return line.mapIndexed { index, string ->
          "!" + (index + 1) + "!" to string
        }.toMap()
      }

      override fun get(): T {
        TODO("Not yet implemented")
      }

      override fun accept(v: String): String {
        TODO("Not yet implemented")
      }
    }
  }

  override fun <K> accept(req: HttpRequest, resp: HttpResponse, data: RequestData<K>) {
    val newResults = acceptor(resp, data as RequestData<T>)
    storage.addAll(newResults)
  }
}
