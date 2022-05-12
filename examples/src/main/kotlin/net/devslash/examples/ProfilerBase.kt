package net.devslash.examples

import net.devslash.*
import net.devslash.data.ListDataSupplier
import net.devslash.decorators.Progress

fun main() {
  System.setProperty("HTTP_THREAD_POOL_SIZE", "50")
  val sManagerSupplier = HttpSessionManager(HttpDriver(StaticHttpAdapter()))
  val session = { num: Int ->
    SessionBuilder().apply {
      concurrency = num
      call<Int>("http://any") {
        install(Progress(1))
        data = ListDataSupplier(lazy {
          println("Evaluated")
          (1..100000).toList()
        })
        before {
        }
        after {
          action { }
        }
      }
    }.build()
  }

  val sesToRun = session(300)
  println("Session created")
  sManagerSupplier.run(sesToRun)
  sManagerSupplier.close()
}
