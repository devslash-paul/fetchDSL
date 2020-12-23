package net.devslash.post

import net.devslash.*

class DefaultResponseFormat : OutputFormat {
  override fun <T> accept(resp: HttpResponse, rep: RequestData<T>): ByteArray? {
    return "Resp ${resp.url} -> ${resp.statusCode}".toByteArray()
  }
}

class LogResponse(private val format: OutputFormat = DefaultResponseFormat()) : FullDataAfterHook {
  override fun <T> accept(req: HttpRequest, resp: HttpResponse, data: RequestData<T>) {
    format.accept(resp, data)?.let {
      println(String(it))
    }
  }
}
