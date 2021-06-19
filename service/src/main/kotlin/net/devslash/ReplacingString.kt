package net.devslash

class ReplacingString<V>(private val inString: String) : RequestVisitor<String, V> {
  override fun invoke(p1: V, classType: Class<*>): String {
    return when (p1) {
      is List<*> -> mapListType(p1)
      else -> throw RuntimeException("ReplacingString requires non null list type, was $classType")
    }
  }

  private fun mapListType(list: List<*>): String {
    if (list.isEmpty() || !inString.contains("!")) {
      return inString
    }
    val mappings = list.mapIndexed { ind, it ->
      "!${ind + 1}!" to it.toString()
    }
    var copy = inString
    mappings.forEach {
      copy = copy.replace(it.first, it.second)
    }
    return copy
  }
}