package net.devslash.outputs

import net.devslash.*
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.io.OutputStream

class AppendFile(
  private val fileName: String,
  private val out: OutputFormat = DefaultOutput()
) : BasicOutput {

  private val lock = Object()
  private var memoizedFile: OutputStream? = null

  init {
    // TODO: This file relies on the string replacement engine
    memoizedFile = if (fileName.contains(Regex("!\\d+!"))) {
      // then it's non memoizable
      null
    } else {
      BufferedOutputStream(FileOutputStream(fileName, true))
    }
  }

  override fun accept(req: HttpRequest, resp: HttpResponse, data: RequestData<*>) {
    val filename = data.visit(ReplacingString(fileName))
    val f = memoizedFile ?: BufferedOutputStream(
      FileOutputStream(
        filename, true
      )
    )

    val output = out.accept(resp, data)
    if (output != null) {
      synchronized(lock) {
        f.write(output)
        f.write(newline)
        f.flush()
        if (memoizedFile == null) {
          // if we're doing it on a per call basis, close the stream
          f.close()
        }
      }
    }
  }

  private val newline = "\n".toByteArray()
}
