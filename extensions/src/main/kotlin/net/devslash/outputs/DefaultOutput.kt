package net.devslash.outputs

import net.devslash.HttpResponse
import net.devslash.OutputFormat
import net.devslash.RequestData

class DefaultOutput: OutputFormat {
  override fun <T> accept(resp: HttpResponse, rep: RequestData<T>): ByteArray {
    return resp.body
  }
}
