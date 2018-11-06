package net.devslash

class ListBasedRequestData(private val parts: List<String> = listOf()) : RequestData {
  override fun getReplacements(): Map<String, String> {
    return parts.mapIndexed { index, string ->
      "!" + (index + 1) + "!" to string
    }.toMap()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ListBasedRequestData

    if (parts != other.parts) return false

    return true
  }

  override fun hashCode(): Int {
    return parts.hashCode()
  }
}
