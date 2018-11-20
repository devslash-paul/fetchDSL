package net.devslash.examples

import io.ktor.application.call
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import net.devslash.data.FileDataSupplier
import net.devslash.outputs.FileOut
import net.devslash.pipes.ResettablePipe
import net.devslash.runHttp
import java.net.ServerSocket
import java.util.concurrent.TimeUnit

fun main() {
  val pipe = ResettablePipe({ r, d -> listOf(String(r.body)) })
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
      data = FileDataSupplier(this.javaClass.getResource("/in.log").path)
      output {
        +pipe
        +FileOut("!1!")
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
