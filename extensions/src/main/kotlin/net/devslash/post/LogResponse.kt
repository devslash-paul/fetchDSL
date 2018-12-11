package net.devslash.post

import net.devslash.*

class DefaultResponseFormat : OutputFormat {
  override fun accept(resp: HttpResponse, rep: RequestData): ByteArray? {
    return "Resp ${resp.url} -> ${resp.statusCode}".toByteArray()
  }
}

class LogResponse(private val format: OutputFormat = DefaultResponseFormat()) : FullDataAfterHook {
  override fun accept(req: HttpRequest, resp: HttpResponse, data: RequestData) {
    format.accept(resp, data)?.let {
      println(String(it))
    }
  }
}
