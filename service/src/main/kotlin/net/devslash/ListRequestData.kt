package net.devslash

class ListRequestData<T>(private val parts: T, private val type: Class<T>) : RequestData<T>() {
  companion object {
    inline operator fun <reified T> invoke(parts: T): ListRequestData<T> {
      return ListRequestData(parts, T::class.java)
    }

    fun <T> invoke(parts: T, clazz: Class<T>): ListRequestData<T> {
      return ListRequestData(parts, clazz)
    }
  }

  override fun <T> visit(visitor: RequestVisitor<T, Any?>): T = visitor(parts, type)

  override fun get(): T {
    return parts
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ListRequestData<*>

    if (parts != other.parts) return false

    return true
  }

  override fun hashCode(): Int {
    return parts.hashCode()
  }
}
