package net.devslash.outputs

import net.devslash.*
import java.io.File

class AppendFile(private val fileName: String,
                 private val out: OutputFormat = DefaultOutput()) : BasicOutput {

  private val lock = Object()
  private var memoizedFile: File? = null

  init {
    if(fileName.contains(Regex("!\\d+!"))) {
      // then it's non memoizable
      memoizedFile = null
    } else {
      memoizedFile = File(fileName)
    }
  }

  override fun accept(req: HttpRequest, resp: HttpResponse, data: RequestData) {
    val f = memoizedFile ?: File(fileName.asReplaceableValue().get(data))
    val output = out.accept(resp, data)
    if (output != null) {
      synchronized(lock) {
        f.appendBytes(output)
        f.appendBytes("\n".toByteArray())
      }
    }
  }
}
