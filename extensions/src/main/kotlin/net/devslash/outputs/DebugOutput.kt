package net.devslash.outputs

import net.devslash.HttpResponse
import net.devslash.OutputFormat
import net.devslash.RequestData
import net.devslash.mustGet

class DebugOutput : OutputFormat {
  override fun accept(resp: HttpResponse, rep: RequestData): ByteArray? {
    return """
              ----------------
              url: ${resp.url}
              status: ${resp.statusCode}
              headers: [${resp.headers}]
              data: ${rep.mustGet<Any?>()}
              body ->
              ${String(resp.body)}
              ----------------
           """.trimIndent().toByteArray()
  }
}
