package net.devslash.runner

import com.github.kittinunf.fuel.core.Method
import net.devslash.SimplePostHook
import net.devslash.SimplePreHook
import java.net.URL
import java.net.URLEncoder

interface RequestDecorator {
  suspend fun accept(session: SessionManager,
                     cookie: CookieJar,
                     req: HttpRequest,
                     data: RequestData) {
    accept(req, data)
  }

  suspend fun accept(req: HttpRequest, data: RequestData) {}
}

interface ResponseConsumer {
  fun accept(resp: HttpResponse)
}


class HttpRequest(val type: Method, val url: String, var body: String) {
  val headers = mutableMapOf<String, String>()

  fun addHeader(name: String, value: String) {
    headers[name] = value
  }

  fun addFormParameter(name: String, value: String) {
    // Lets just randomly add it ay
    body = body + "&" + name + "=" + URLEncoder.encode(value)
  }
}

data class HttpResponse(var url: URL, val statusCode: Int, val headers: Map<String, List<String>>, var body: ByteArray)

class CookieJar : SimplePreHook, SimplePostHook {
  // always send all cookies
  private val cookies = mutableMapOf<String, String>()

  override suspend fun accept(req: HttpRequest, data: RequestData) {
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
