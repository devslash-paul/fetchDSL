package net.devslash.data

import net.devslash.RequestData
import net.devslash.RequestDataSupplier
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class FileDataSupplier(val name: String, val split: String = " ") : RequestDataSupplier {
  val sourceFile = File(name).readLines()
  val line = AtomicInteger(0)

  override fun getDataForRequest(): RequestData {
    return object : RequestData {
      val ourLine = sourceFile[line.getAndIncrement()].split(split)
      override fun getReplacements(): Map<String, String> {
        return ourLine.mapIndexed { index, string ->
          "!" + (index + 1) + "!" to string
        }.toMap()
      }
    }
  }

  override fun hasNext(): Boolean {
    val lineNum = line.get()
    // Check if we're either at the end of the file, or the second last line and it's an empty line
    return lineNum < sourceFile.size && !(sourceFile[lineNum].isEmpty() && lineNum == sourceFile.size - 1)
  }

}
