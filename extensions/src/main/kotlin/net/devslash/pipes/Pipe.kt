package net.devslash.pipes

import net.devslash.*
import java.util.concurrent.ConcurrentLinkedDeque

class Pipe(val acceptor: (HttpResponse, RequestData) -> List<String>, private val split: String? = null) : BasicOutput, RequestDataSupplier {

  private val storage = ConcurrentLinkedDeque<String>()

  override suspend fun getDataForRequest(): RequestData? {
    val currentValue = storage.poll() ?: return null
    val line = if (split != null) {
      currentValue.split(split)
    } else listOf(currentValue)

    return object : RequestData {
      override fun getReplacements(): Map<String, String> {
        return line.mapIndexed { index, string ->
          "!" + (index + 1) + "!" to string
        }.toMap()
      }
    }
  }

  override fun accept(req: HttpRequest, resp: HttpResponse, data: RequestData) {
    val newResults = acceptor(resp, data)
    storage.addAll(newResults)
  }
}
