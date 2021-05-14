package net.devslash

private class OverwrittenUrlProvider(
  private val url: String,
  private val data: RequestData
) : URLProvider {
  override fun get(): String {
    if (url.contains("!")) {
      return data.visit(ReplacingString(url))
    }
    return url
  }
}

fun getUrlProvider(call: Call<*>, data: RequestData): URLProvider {
  return OverwrittenUrlProvider(call.url, data)
}
