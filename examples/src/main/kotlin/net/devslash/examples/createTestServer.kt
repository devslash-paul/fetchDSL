package net.devslash.examples

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.net.ServerSocket

fun createTestServer(): Pair<NettyApplicationEngine, String> {
  val port = ServerSocket(0).use { it.localPort }
  val server = embeddedServer(Netty, port) {
      routing {
          get("/") {
              call.respondText("response here")
          }
      }
  }
  server.start()
  val address = "http://localhost:$port"
  return Pair(server, address)
}
