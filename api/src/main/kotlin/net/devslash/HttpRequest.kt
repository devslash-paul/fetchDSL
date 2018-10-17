package net.devslash

import com.github.kittinunf.fuel.core.Method
import java.net.URL
import java.net.URLEncoder

interface RequestDecorator {
  suspend fun accept(httpSession: SessionManager,
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
    body = body + "&" + name + "=" + URLEncoder.encode(value, "UTF-8")
  }
}

data class HttpResponse(var url: URL, val statusCode: Int, val headers: Map<String, List<String>>, var body: ByteArray)

