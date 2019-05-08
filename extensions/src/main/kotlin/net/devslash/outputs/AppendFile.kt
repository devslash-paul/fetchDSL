package net.devslash.outputs

import net.devslash.*
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.io.OutputStream

class AppendFile(private val fileName: String,
                 private val out: OutputFormat = DefaultOutput()) : BasicOutput {

  private val lock = Object()
  private var memoizedFile: OutputStream? = null

  init {
    if (fileName.contains(Regex("!\\d+!"))) {
      // then it's non memoizable
      memoizedFile = null
    } else {
      memoizedFile = BufferedOutputStream(FileOutputStream(fileName, true))
    }
  }

  override fun accept(req: HttpRequest, resp: HttpResponse, data: RequestData) {
    val f = memoizedFile ?: BufferedOutputStream(FileOutputStream(fileName.asReplaceableValue().get(
        data), true))

    val output = out.accept(resp, data)
    if (output != null) {
      synchronized(lock) {
        f.write(output)
        f.write(NEWLINE)
        f.flush()
        if (memoizedFile == null) {
          // if we're doing it on a per call basis, close the stream
          f.close()
        }
      }
    }
  }

  companion object {
    private val NEWLINE = "\n".toByteArray()
  }
}
