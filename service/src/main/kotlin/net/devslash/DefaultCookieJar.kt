package net.devslash

import java.net.HttpCookie
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

class DefaultCookieJar : CookieJar {
  private val cookies = ConcurrentHashMap(mutableMapOf<String, MutableMap<String, String>>())

  override fun accept(req: HttpRequest, data: RequestData<*>) {
    val basicURl = URI(req.url)
    val filteredUrl = "${basicURl.scheme}://${basicURl.host}"

    val siteCookies = cookies[filteredUrl]
    siteCookies?.let { found ->
      // defensive copy
      val cookies = found.toList()
      val cookie = cookies.joinToString("; ") { "${it.first}=${it.second}" }
      req.addHeader("Cookie", cookie)
    }
  }

  override fun accept(resp: HttpResponse) {
    val filteredUrl = "${resp.uri.scheme}://${resp.uri.host}"
    val setCookie = resp.headers.filterKeys { it.equals("Set-Cookie", true) }

    val foundCookies = mutableListOf<HttpCookie>()
    for (value in setCookie.values) {
      for (cookie in value) {
        foundCookies += HttpCookie.parse(cookie)
      }
    }

    for (cookie in foundCookies) {
      val li = cookies.computeIfAbsent(filteredUrl) { ConcurrentHashMap() }
      li[cookie.name] = cookie.value
    }
  }
}
