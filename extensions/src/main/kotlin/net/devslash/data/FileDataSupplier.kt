package net.devslash.data

import net.devslash.ListBasedRequestData
import net.devslash.RequestData
import net.devslash.RequestDataSupplier
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class FileDataSupplier(val name: String, private val split: String = " ") : RequestDataSupplier<List<String>> {
  private val sourceFile = File(name).readLines()
  private val line = AtomicInteger(0)

  override suspend fun getDataForRequest(): RequestData<List<String>>? {
    val ourLine = sourceFile.getOrNull(line.getAndIncrement())?.split(split)
    return if (ourLine == null) null else ListBasedRequestData(ourLine)
  }
}
