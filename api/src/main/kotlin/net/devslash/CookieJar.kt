package net.devslash

import java.net.URL

class CookieJar : SimpleBeforeHook, SimpleAfterHook {
  private val cookies = mutableMapOf<URL, MutableMap<String, String>>()

  override fun accept(req: HttpRequest, data: RequestData) {
    val basicURl = URL(req.url)
    val filteredUrl = URL("${basicURl.protocol}://${basicURl.host}")
    val siteCookies = cookies[filteredUrl]

    siteCookies?.let {
      val cookie = siteCookies
          .map { "${it.key}=${it.value}" }
          .joinToString("; ")
      req.addHeader("Cookie", cookie)
    }
  }

  override fun accept(resp: HttpResponse) {
    // we need to sort via the domain name + the security
    val filteredUrl = URL("${resp.url.protocol}://${resp.url.host}")
    resp.headers.filterKeys { it.equals("Set-Cookie", true) }
        .forEach {
          it.value.forEach { a ->
            val cook = a.split("=")
            if (filteredUrl !in cookies.keys) {
              cookies[filteredUrl] = mutableMapOf()
            }
            cookies[filteredUrl]!![cook[0]] = cook[1].split(";")[0]
          }
        }
  }
}
