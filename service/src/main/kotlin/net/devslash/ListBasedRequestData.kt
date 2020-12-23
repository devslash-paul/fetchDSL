package net.devslash

/**
 * This is a one-shot. Not a data supplier. Therefore this simply has to accept a list and provide it in subsequent
 * calls
 */
public class ListBasedRequestData<E, T : List<E>> private constructor(
  private val parts: T,
) : RequestData<T> {


  public companion object {
    public operator fun <T> invoke(list: List<T>): ListBasedRequestData<T, List<T>> {
      return ListBasedRequestData(list);
    }
  }

  override fun getReplacements(): Map<String, String> {
    return parts.mapIndexed { index, obj ->
      "!" + (index + 1) + "!" to obj.toString()
    }.toMap()
  }

  override fun get(): T = parts
  override fun accept(v: String): String {
    var x = v
    parts.forEachIndexed { index, e -> x = x.replace("!${index + 1}!", e.toString()) }
    return x
  }
}
