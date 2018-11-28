package net.devslash

class CookieJar : SimpleBeforeHook, SimpleAfterHook {
  // always send all cookies
  private val cookies = mutableMapOf<String, String>()

  override fun accept(req: HttpRequest, data: RequestData) {
    val cookie = cookies
        .map { "${it.key}=${it.value}" }
        .joinToString("; ")
    req.addHeader("Cookie", cookie)
  }

  override fun accept(resp: HttpResponse) {
    resp.headers.filterKeys { it.equals("Set-Cookie", true) }
        .forEach {
          it.value.forEach { a ->
            val cook = a.split("=")
            cookies[cook[0]] = cook[1].split(";")[0]
          }
        }
  }
}
