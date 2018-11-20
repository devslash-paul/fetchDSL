package net.devslash

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
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
