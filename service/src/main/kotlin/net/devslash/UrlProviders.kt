package net.devslash

private class OverwrittenUrlProvider<T>(
  private val url: String,
  private val replacements: RequestData<T>
) : URLProvider {
  override fun get(): String {
    return replacements.accept(url)
  }
}

internal fun <T> getUrlProvider(call: Call<T>, data: RequestData<T>): URLProvider {
  return OverwrittenUrlProvider(call.url, data)
}
