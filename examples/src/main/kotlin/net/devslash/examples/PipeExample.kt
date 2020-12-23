package net.devslash.examples

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import net.devslash.data.ListDataSupplier
import net.devslash.pipes.ResettablePipe
import net.devslash.runHttp
import java.net.ServerSocket
import java.util.concurrent.TimeUnit

fun main() {
  val pipe = ResettablePipe<String> { r, _ -> listOf(String(r.body)) }
  val port = ServerSocket(0).use { it.localPort }
  val server = embeddedServer(Netty, port) {
    routing {
      get("/") {
        call.respondText("")
      }
    }
  }
  server.start()
  val address = "http://localhost:$port"
  runHttp {
    call(address) {
      // Ok this asserting that it passesa round List types as its big object. therefore this should
      data = ListDataSupplier(listOf("1"))
      after {
        +pipe
//        +WriteFileeFile<Int>("!1!")
      }
    }
    call(address) {
      data = pipe
    }
    call(address) {
      data = pipe
    }
  }

  server.stop(100, 100, TimeUnit.MILLISECONDS)
}
