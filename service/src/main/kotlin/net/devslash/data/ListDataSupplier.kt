package net.devslash.data

import net.devslash.ListRequestData
import net.devslash.RequestData
import net.devslash.RequestDataSupplier
import java.util.concurrent.atomic.AtomicInteger

class ListDataSupplier<T>(private val list: Lazy<List<T>>, private val clazz: Class<T>) : RequestDataSupplier<T> {
  private val line = AtomicInteger(0)

  companion object {
    inline operator fun <reified T> invoke(list: List<T>): ListDataSupplier<T> {
      return ListDataSupplier(lazy { list }, T::class.java)
    }

    // There used to be a way to send a list of strings, and have it go to a list of list of strings
    @JvmName("stringToList")
    operator fun invoke(list: List<String>): ListDataSupplier<List<String>> {
      return ListDataSupplier(list.map { listOf(it) }.toList())
    }

    @JvmName("lazyStringToList")
    operator fun <T> invoke(
      lazyList: Lazy<List<T>>,
      trans: (T) -> List<String>
    ): ListDataSupplier<List<String>> {
      return ListDataSupplier(transformedLazyList(lazyList, trans))
    }

    @JvmName("lazyListStringToList")
    operator fun invoke(
      ll: Lazy<List<String>>
    ): ListDataSupplier<List<String>> {
      return invoke(ll) { listOf(it) }
    }

    private fun <T> transformedLazyList(
      lazyList: Lazy<List<T>>,
      transform: (T) -> List<String>
    ): Lazy<List<List<String>>> {
      // TODO: Do i really need to do this?? Feels weird
      return lazy {
        object : List<List<String>> {
          override fun contains(element: List<String>): Boolean = throw UnsupportedOperationException()
          override fun containsAll(elements: Collection<List<String>>) = throw UnsupportedOperationException()
          override fun isEmpty(): Boolean = lazyList.value.isEmpty()
          override fun indexOf(element: List<String>): Int = throw UnsupportedOperationException()
          override fun iterator(): Iterator<List<String>> = throw UnsupportedOperationException()
          override fun lastIndexOf(element: List<String>): Int = throw UnsupportedOperationException()
          override fun listIterator(): ListIterator<List<String>> = throw UnsupportedOperationException()
          override fun listIterator(index: Int): ListIterator<List<String>> = throw UnsupportedOperationException()
          override fun subList(fromIndex: Int, toIndex: Int): List<List<String>> = throw UnsupportedOperationException()

          override val size: Int
            get() = lazyList.value.size

          override fun get(index: Int): List<String> = transform(lazyList.value[index])
        }
      }
    }

    @Deprecated(
      "Instead of transforming, send type directly",
      ReplaceWith("ListDataSupplier(list)", "net.devslash.data.ListDataSupplier")
    )
    inline operator fun <reified T, Q> invoke(list: List<Q>, transform: (Q) -> T): ListDataSupplier<T> {
      return ListDataSupplier(list.map { transform(it) }.toList())
    }

    inline operator fun <reified T> invoke(list: Lazy<List<T>>): ListDataSupplier<T> {
      return ListDataSupplier(list, T::class.java)
    }

    @Suppress("unused")
    inline fun <reified T> single(item: T): ListDataSupplier<T> {
      return ListDataSupplier(lazy { listOf(item) }, T::class.java)
    }
  }

  override suspend fun getDataForRequest(): RequestData<T>? {
    val index = line.getAndIncrement()
    val obj = list.value.getOrNull(index) ?: return null
    return ListRequestData(obj, clazz)
  }
}
