package net.devslash.outputs

import net.devslash.HttpResponse
import net.devslash.OutputFormat
import net.devslash.RequestData

class DebugOutput : OutputFormat {
  override fun <T> accept(resp: HttpResponse, rep: RequestData<T>): ByteArray? {
    return """
              ----------------
              url: ${resp.url}
              status: ${resp.statusCode}
              headers: [${resp.headers}]
              data: ${rep.getReplacements()}
              body ->
              ${String(resp.body)}
              ----------------
           """.trimIndent().toByteArray()
  }
}
