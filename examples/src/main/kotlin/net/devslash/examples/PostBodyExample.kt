package net.devslash.examples

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import net.devslash.HttpMethod
import net.devslash.outputs.DebugOutput
import net.devslash.outputs.StdOut
import net.devslash.post.LogResponse
import net.devslash.pre.LogRequest
import net.devslash.runHttp
import java.net.ServerSocket

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
}
