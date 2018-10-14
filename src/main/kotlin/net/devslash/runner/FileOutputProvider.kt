package net.devslash.runner

import java.io.File

class FileOutputProvider(private val filename: String,
                         private val append: Boolean,
                         private val binary: Boolean) : OutputHandler {
  override fun output(resp: HttpResponse, data: RequestData) {
    val file = File(getReplacements(filename, data.getReplacements()))

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

  private fun getReplacements(filename: String, replacements: Map<String, String>): String? {
    var copy = "" + filename

    replacements.forEach { key, value -> copy = copy.replace(key, value) }
    return copy
  }

  override fun suspectedOutput(data: RequestData): String? {
    return (getReplacements(filename, data.getReplacements()))
  }
}
