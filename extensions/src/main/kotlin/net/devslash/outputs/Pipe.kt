package net.devslash.outputs

import net.devslash.BasicOutput
import net.devslash.HttpResponse
import net.devslash.RequestData
import net.devslash.RequestDataSupplier
import java.util.*

class Pipe(val acceptor: (HttpResponse, RequestData) -> String, val split: String) : BasicOutput, RequestDataSupplier {
  private val storage = ArrayDeque<String>()

  override fun getDataForRequest(): RequestData {
    val line = storage.pop().split(split)
    return object : RequestData {
      override fun getReplacements(): Map<String, String> {
        return line.mapIndexed { index, string ->
          "!" + (index + 1) + "!" to string
        }.toMap()
      }
    }
  }

  override fun hasNext(): Boolean = storage.isNotEmpty()


  override fun accept(resp: HttpResponse, data: RequestData) {
    storage.add(acceptor(resp, data))
  }
}
