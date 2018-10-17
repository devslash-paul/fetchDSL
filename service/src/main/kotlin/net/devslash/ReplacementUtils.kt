package net.devslash

fun replace(replaceable: String, data: RequestData): String {
  val replacements = data.getReplacements()
  var copy = "" + replaceable
  replacements.forEach { key, value -> copy = copy.replace(key, value) }
  return copy

}
