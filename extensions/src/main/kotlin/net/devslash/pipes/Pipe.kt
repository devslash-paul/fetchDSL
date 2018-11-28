package net.devslash.pipes

import net.devslash.*
import java.util.*

class Pipe(val acceptor: (HttpResponse, RequestData) -> List<String>, val split: String? = null) : BasicOutput, RequestDataSupplier {

  private val storage = ArrayDeque<String>()

  override fun getDataForRequest(): RequestData {
    val currentValue = storage.pop()
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

  override fun hasNext(): Boolean = storage.isNotEmpty()

  override fun accept(req: HttpRequest, resp: HttpResponse, data: RequestData) {
    val newResults = acceptor(resp, data)
    storage.addAll(newResults)
  }
}
