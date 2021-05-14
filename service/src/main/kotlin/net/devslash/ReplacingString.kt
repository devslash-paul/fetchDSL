package net.devslash

class ReplacingString<V>(private val inString: String) : RequestVisitor<String, V> {
  override fun invoke(p1: V, p2: Class<*>): String {
    return when (p2) {
      List::class.java -> mapListType(p1 as List<*>)
      else -> throw RuntimeException("ReplacingString requires list type, was $p2")
    }
  }

  private fun mapListType(list: List<*>): String {
    if (list.isEmpty() || !inString.contains("!")) {
      return inString
    }
    val mappings = list.mapIndexed { ind, it ->
      "!${ind + 1}!" to it.toString()
    }
    var copy = inString + ""
    mappings.forEach {
      copy = copy.replace(it.first, it.second)
    }
    return copy
  }
}