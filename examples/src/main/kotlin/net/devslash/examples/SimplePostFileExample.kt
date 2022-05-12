package net.devslash.examples

import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import net.devslash.data.FileDataSupplier
import net.devslash.runHttp
import java.net.ServerSocket
import java.util.concurrent.TimeUnit

fun main() {
  val port = ServerSocket(0).use { it.localPort }
  val server = embeddedServer(Netty, port) {
    routing {
      post("/") {
      }
    }
  }
  server.start()
  val address = "http://localhost:$port"

  try {
    runHttp {
      call(address) {
        data = FileDataSupplier(this.javaClass.getResource("/in.log")!!.path)
        body {
          value {
            val z: List<String> = it
            z[0]
          }
        }
      }
    }
  } finally {
    server.stop(10, 10, TimeUnit.MILLISECONDS)
  }
}
