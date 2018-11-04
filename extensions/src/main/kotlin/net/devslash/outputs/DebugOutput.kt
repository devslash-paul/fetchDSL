package net.devslash.outputs

import net.devslash.HttpResponse
import net.devslash.OutputFormat
import net.devslash.RequestData

class DebugOutput : OutputFormat {
  override fun accept(f: HttpResponse, rep: RequestData): ByteArray? {
    return """
              ----------------
              url: ${f.url}
              status: ${f.statusCode}
              headers: [${f.headers}]
              data: ${rep.getReplacements()}
              body ->
              ${String(f.body)}
              ----------------
           """.trimIndent().toByteArray()
  }
}
