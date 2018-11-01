package net.devslash

private class OverwrittenUrlProvider(private val url: String,
                                     private val replacements: RequestData) : URLProvider {
  override fun get(): String {
    return url.asReplaceableValue().get(replacements)
  }
}

fun getUrlProvider(call: Call, data: RequestData): URLProvider {
  return OverwrittenUrlProvider(call.url, data)
}

