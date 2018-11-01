package net.devslash.outputs

import net.devslash.BasicOutput
import net.devslash.HttpResponse
import net.devslash.RequestData
import net.devslash.asReplaceableValue
import java.io.File

interface OutputType {
  fun accept(f: HttpResponse, rep: RequestData): ByteArray?
}

class DefaultOutput : OutputType {
  override fun accept(f: HttpResponse, rep: RequestData): ByteArray? {
    return f.body
  }
}

class FileOut(private val fileName: String, val out: OutputType = DefaultOutput()) : BasicOutput {
  val lock = Object()

  override fun accept(resp: HttpResponse, repl: RequestData) {
    synchronized(lock) {
      val f = File(fileName.asReplaceableValue().get(repl))
      val output = out.accept(resp, repl)
      if (output != null) {
        f.writeBytes(output)
      }
    }
  }
}
