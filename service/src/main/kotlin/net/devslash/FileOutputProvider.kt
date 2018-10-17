package net.devslash

import java.io.File

class FileOutputProvider(private val filename: String,
                         private val append: Boolean,
                         private val binary: Boolean) : OutputHandler {
  override fun output(resp: HttpResponse, data: RequestData) {
    val file = File(replace(filename, data))

    when {
      binary -> {
        file.createNewFile()
        file.writeBytes(resp.body)
      }
      append -> synchronized(this) { file.appendText(String(resp.body) + "\n") }
      else -> {
        file.createNewFile()
        file.writeText(String(resp.body))
      }
    }
  }

  override fun suspectedOutput(data: RequestData): String? {
    return (replace(filename, data))
  }
}
