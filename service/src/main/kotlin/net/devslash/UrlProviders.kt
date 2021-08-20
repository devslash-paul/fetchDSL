package net.devslash

fun defaultUrlProvider(url: String, data: RequestData<*>): String {
  if (url.contains("!")) {
    return data.visit(ReplacingString(url))
  }
  return url
}

fun getUrlProvider(call: Call<*>): URLProvider {
  if (call.urlProvider == null) {
    return ::defaultUrlProvider
  }
  return call.urlProvider!!
}
