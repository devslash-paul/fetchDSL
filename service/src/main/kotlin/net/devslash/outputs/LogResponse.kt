package net.devslash.outputs

import net.devslash.*

class DefaultResponseFormat : OutputFormat {
  override fun accept(resp: HttpResponse, data: RequestData): ByteArray {
    return "Resp ${resp.uri} -> ${resp.statusCode}".toByteArray()
  }
}

@Suppress("unused")
class LogResponse(private val format: OutputFormat = DefaultResponseFormat()) : FullDataAfterHook {
  override fun accept(req: HttpRequest, resp: HttpResponse, data: RequestData) {
    format.accept(resp, data)?.let {
      println(String(it))
    }
  }
}
