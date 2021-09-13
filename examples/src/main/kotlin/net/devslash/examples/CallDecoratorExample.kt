package net.devslash.examples

import net.devslash.Call
import net.devslash.CallDecorator
import net.devslash.action
import net.devslash.runHttp

class HttpDowngrader<T> : CallDecorator<T> {
  override fun accept(call: Call<T>): Call<T> {
    return Call(call.url.replace(Regex("^https"), "http"), call.urlProvider,
        call.concurrency, call.headers, call.type, call.dataSupplier, call.body, call.onError,
        call.beforeHooks, call.afterHooks)
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
