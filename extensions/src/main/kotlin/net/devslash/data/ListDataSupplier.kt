package net.devslash.data

import net.devslash.ListBasedRequestData
import net.devslash.RequestData
import net.devslash.RequestDataSupplier
import java.util.concurrent.atomic.AtomicInteger

class ListDataSupplier<T>(private val list: List<T>,
                          private val transform: (T) -> List<String>) : RequestDataSupplier {
  private val line = AtomicInteger(0)

  override fun getDataForRequest(): RequestData {
    val index = line.getAndIncrement()
    val obj = list[index]
    return ListBasedRequestData(transform(obj))
  }

  override fun hasNext(): Boolean {
    val lineNum = line.get()
    // Check if we're either at the end of the file, or the second last line and it's an empty line
    return lineNum < list.size - 1
  }

}
