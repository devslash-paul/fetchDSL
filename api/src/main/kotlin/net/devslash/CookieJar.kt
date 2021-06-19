package net.devslash

import java.net.HttpCookie
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

// TODO: Remove from FetchDSL api
// TODO: Make it easier to cache this inbetween runHTTP Blocks
class CookieJar : SimpleBeforeHook, SimpleAfterHook {
  private val cookies = ConcurrentHashMap(mutableMapOf<String, MutableMap<String, String>>())

  override fun accept(req: HttpRequest, data: RequestData) {
    val basicURl = URL(req.url)
    val filteredUrl = "${basicURl.protocol}://${basicURl.host}"

    synchronized(this) {
      val siteCookies = cookies[filteredUrl]

      siteCookies?.let { found ->
        // defensive copy
        val cookies = found.toList()
        val cookie = cookies.joinToString("; ") { "${it.first}=${it.second}" }
        req.addHeader("Cookie", cookie)
      }
    }
  }

  override fun accept(resp: HttpResponse) {
    // we need to sort via the domain name + the security
    val filteredUrl = "${resp.url.scheme}://${resp.url.host}"
    val setCookie = resp.headers.filterKeys { it.equals("Set-Cookie", true) }
    setCookie.flatMap { it.value }
      .map {
        HttpCookie.parse(it)
      }
      .forEach {
        val li = cookies.computeIfAbsent(filteredUrl) { ConcurrentHashMap() }
        li.putAll(it.map { cval -> cval.name to cval.value })
      }
  }
}
