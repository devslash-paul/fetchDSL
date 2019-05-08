package net.devslash.data

import net.devslash.ListBasedRequestData
import net.devslash.RequestData
import net.devslash.RequestDataSupplier
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class FileDataSupplier(val name: String, private val split: String = " ") : RequestDataSupplier {
  private val sourceFile = File(name).readLines()
  private val line = AtomicInteger(0)

  override fun getDataForRequest(): RequestData {
    val ourLine = sourceFile[line.getAndIncrement()].split(split)
    return ListBasedRequestData(ourLine)
  }

  override fun hasNext(): Boolean {
    val lineNum = line.get()
    // Check if we're either at the end of the file, or the second last line and it's an empty line
    return lineNum < sourceFile.size && !(sourceFile[lineNum].isEmpty() && lineNum == sourceFile.size - 1)
  }
}
