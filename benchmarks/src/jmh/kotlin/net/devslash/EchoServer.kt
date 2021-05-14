package net.devslash

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.io.Closeable
import java.net.ServerSocket
import java.util.concurrent.TimeUnit

class EchoServer : Closeable {
  private val port = ServerSocket(0).use { it.localPort }
  private val server = embeddedServer(Netty, port) {
    routing {
      get("/") {
        headersOf("Content-type", "text/plain")
        call.respond(HttpStatusCode.OK, call.receiveText())
      }
    }
  }
  val address: String = "http://localhost:$port/"

  fun start() {
    server.start()
  }

  override fun close() {
    server.stop(100, 100, TimeUnit.MILLISECONDS)
  }
}
