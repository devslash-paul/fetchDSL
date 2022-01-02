package net.devslash.examples

import net.devslash.HttpDriver
import net.devslash.HttpSessionManager
import net.devslash.SessionBuilder
import net.devslash.action
import net.devslash.data.ListDataSupplier

fun main() {
  System.setProperty("HTTP_THREAD_POOL_SIZE", "50")
  val sManagerSupplier = HttpSessionManager(HttpDriver(MockHttpAdapter()))
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
