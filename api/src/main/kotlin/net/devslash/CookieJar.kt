package net.devslash

import java.net.URL
import java.util.concurrent.ConcurrentHashMap

class CookieJar : SimpleBeforeHook, SimpleAfterHook {
  private val cookies = ConcurrentHashMap(mutableMapOf<String, MutableMap<String, String>>())

  override fun accept(req: HttpRequest, data: RequestData) {
    val basicURl = URL(req.url)
    val filteredUrl = "${basicURl.protocol}://${basicURl.host}"

    synchronized(this) {
      val siteCookies = cookies[filteredUrl]

      siteCookies?.let {
        // defensive copy
        val cookies = HashMap(it)
        val cookie = cookies.map { "${it.key}=${it.value}" }.joinToString("; ")
        req.addHeader("Cookie", cookie)
      }
    }
  }

  override fun accept(resp: HttpResponse) {
    // we need to sort via the domain name + the security
    val filteredUrl = "${resp.url.protocol}://${resp.url.host}"
    resp.headers.filterKeys { it.equals("Set-Cookie", true) }
        .forEach {
          it.value.forEach { a ->
            val cook = a.split("=")
            if (filteredUrl !in cookies.keys) {
              synchronized(this) {
                cookies[filteredUrl] = ConcurrentHashMap(mutableMapOf())
              }
            }
            cookies[filteredUrl]!![cook[0]] = cook[1].split(";")[0]
          }
        }
  }
}
