package net.devslash

fun <T> defaultUrlProvider(url: String, data: RequestData<T>): String {
  if (url.contains("!")) {
    return data.visit(ReplacingString(url))
  }
  return url
}

fun <T> getUrlProvider(call: Call<T>): URLProvider<T> {
  if (call.urlProvider == null) {
    return ::defaultUrlProvider
  }
  return call.urlProvider!!
}
