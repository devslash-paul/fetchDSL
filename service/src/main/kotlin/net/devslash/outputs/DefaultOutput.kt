package net.devslash.outputs

import net.devslash.HttpResponse
import net.devslash.OutputFormat
import net.devslash.RequestData

class DefaultOutput : OutputFormat {
  override fun accept(resp: HttpResponse, data: RequestData<*>): ByteArray {
    return resp.body
  }
}
