package net.devslash.outputs

import net.devslash.HttpResponse
import net.devslash.OutputFormat
import net.devslash.RequestData

class DefaultOutput : OutputFormat {
  override fun accept(resp: HttpResponse, rep: RequestData): ByteArray? {
    return resp.body
  }
}
