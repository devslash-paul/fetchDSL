package net.devslash.runner

import net.devslash.Call

private class OverwrittenUrlProvider(private val url: String,
                                     private val replacements: Map<String, String>) : URLProvider {
  override fun get(): String {
    var urlCopy = "" + url
    replacements.forEach { key, value -> urlCopy = urlCopy.replace(key, value)}
    return urlCopy
  }
}

fun getUrlProvider(call: Call, data: RequestData): URLProvider {
  return OverwrittenUrlProvider(call.url, data.getReplacements())
}

