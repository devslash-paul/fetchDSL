package net.devslash.outputs

import net.devslash.*
import java.io.File

class AppendFile(private val fileName: String, private val out: OutputFormat = DefaultOutput()) :
  BasicOutput {
  private val lock = Object()

  override fun accept(resp: HttpResponse, data: RequestData) {
    synchronized(lock) {
      val f = File(fileName.asReplaceableValue().get(data))
      val output = out.accept(resp, data)
      if (output != null) {
        f.appendBytes(output)
        f.appendBytes("\n".toByteArray())
      }
    }
  }
}
