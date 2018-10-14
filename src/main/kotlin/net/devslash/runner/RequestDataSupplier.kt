package net.devslash.runner

import net.devslash.InputFile
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

interface RequestDataSupplier {
  /**
   * Request data should be a closure that is safe to call on a per-request basis
   */
  fun getDataForRequest(): RequestData

  fun hasNext(): Boolean
}

interface RequestData {
  fun getReplacements(): Map<String, String>
}

class FileBasedDataSupplier(private val inFile: InputFile) : RequestDataSupplier {
  val sourceFile = File(inFile.name).readLines()
  val line = AtomicInteger(0)

  override fun getDataForRequest(): RequestData {
    return object : RequestData {
      val ourLine = sourceFile[line.getAndIncrement()].split(inFile.split)
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
