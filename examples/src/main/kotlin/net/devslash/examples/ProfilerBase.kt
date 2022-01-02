package net.devslash.examples

import net.devslash.*
import net.devslash.data.ListDataSupplier

fun main() {
  System.setProperty("HTTP_THREAD_POOL_SIZE", "50")
  val sManagerSupplier = HttpSessionManager(HttpDriver(StaticHttpAdapter()))
  val session = { num: Int ->
    SessionBuilder().apply {
      concurrency = num
      call<Int>("http://any") {
        data = ListDataSupplier((1..100000).toList())
        before {
        }
        after {
          action {  }
        }
      }
    }.build()
  }

  sManagerSupplier.run(session(300))
  sManagerSupplier.close()
}
