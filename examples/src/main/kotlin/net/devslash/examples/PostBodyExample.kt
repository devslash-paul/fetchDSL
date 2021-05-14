package net.devslash.examples

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import net.devslash.HttpMethod
import net.devslash.outputs.DebugOutput
import net.devslash.outputs.StdOut
import net.devslash.pre.LogRequest
import net.devslash.runHttp
import java.net.ServerSocket
import java.util.concurrent.TimeUnit

fun main() {
  val port = ServerSocket(0).use { it.localPort }
  val server = embeddedServer(Netty, port) {
    routing {
      post("/") {
        call.respond(HttpStatusCode.OK, "Response Body Text")
      }
    }
  }
  server.start()
  val address = "http://localhost:$port/"

  runHttp {
    call(address) {
      type = HttpMethod.POST
      body {
        formParams(mapOf("Hi" to listOf("ho")))
      }
      before {
        +LogRequest()
      }
      after {
        +StdOut(format = DebugOutput())
      }
    }
  }
  server.stop(100, 100, TimeUnit.MILLISECONDS)
}
