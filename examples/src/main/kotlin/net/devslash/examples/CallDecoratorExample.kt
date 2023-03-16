package net.devslash.examples

import net.devslash.*
import net.devslash.data.ListDataSupplier
import java.time.Duration

class HttpDowngrader<T> : CallDecorator<T> {
  override fun accept(call: Call<T>): Call<T> {
    return Call(call.url.replace(Regex("^https"), "http"), call.urlProvider,
        call.concurrency, call.rateOptions, call.headers, call.type, call.dataSupplier, call.body, call.onError,
        call.beforeHooks, call.afterHooks)
  }
}

fun main2() {
runHttp {
  call("https://example.com/!1!/!2!") {
    rateLimit(10, Duration.ofSeconds(1))
    type = HttpMethod.POST
    headers = mapOf("X-Example" to listOf("exampleValue"))
    body {
      jsonObject = mapOf("key" to "value")
    }
    data = ListDataSupplier(listOf(
        listOf("path1", "a"),
        listOf("path2", "b"),
        listOf("path3", "c")))
  }
}
}

fun main() {
  val (server, address) = createTestServer()
  try {
    val newAddr = address.replace(Regex("^http"), "https")
    println(newAddr)
    runHttp {
      call(newAddr) {
        install(HttpDowngrader(), HttpDowngrader())
        after {
          action {
            println(resp.uri)
          }
        }
      }
    }
  } finally {
    server.stop(100, 100)
  }
}
