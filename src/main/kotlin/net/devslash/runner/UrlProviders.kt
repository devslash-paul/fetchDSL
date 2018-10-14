package net.devslash.runner

import net.devslash.Call

private class OverwrittenUrlProvider(private val url: String,
                                     private val replacements: RequestData) : URLProvider {
  override fun get(): String {
    return replace(url, replacements)
  }
}

fun getUrlProvider(call: Call, data: RequestData): URLProvider {
  return OverwrittenUrlProvider(call.url, data)
}

