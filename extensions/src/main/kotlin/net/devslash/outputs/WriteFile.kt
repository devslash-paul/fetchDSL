package net.devslash.outputs

import net.devslash.*
import java.io.File


class WriteFile<T>(
  private val fileName: String,
  private val out: OutputFormat = DefaultOutput()
) : BasicOutput {
  private val lock = Object()

  override fun <T> accept(req: HttpRequest, resp: HttpResponse, data: RequestData<T>) {
    synchronized(lock) {
      val f = File(data.accept(fileName))
      val output = out.accept(resp, data)
      if (output != null) {
        f.writeBytes(output)
      }
    }
  }
}

