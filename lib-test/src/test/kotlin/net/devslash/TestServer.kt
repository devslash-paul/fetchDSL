package net.devslash

import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import org.junit.rules.ExternalResource
import java.util.concurrent.atomic.AtomicInteger

class TestServer : ExternalResource() {
  private var server: io.ktor.server.netty.NettyApplicationEngine? = null
  private var port: Int = 0
  val count = AtomicInteger(0)
  val address = { "http://localhost:${port}" }

  private fun start() {
    port = 50000
    println("USING PORT $port")
    server =
        embeddedServer(io.ktor.server.netty.Netty, port, configure = { runningLimit = 20 }) {
          routing {
            post("/bounce") {
              bounceResponse()
            }
            get("/bounce") {
              bounceResponse()
            }
            get("/count") {
              call.respond(
                  io.ktor.http.HttpStatusCode.Companion.OK,
                  "" + count.incrementAndGet()
              )
            }
          }
        }
    server!!.start()
  }

  private suspend fun PipelineContext<Unit, ApplicationCall>.bounceResponse() {
    call.request.headers.filter { name, _ -> !listOf("content-length", "content-type").contains(name) }.forEach { key, list ->
      // Filter some
      list.forEach { call.response.headers.append(key, it) }
    }
    val message = call.receive<String>()
    call.respond(message)
  }

  override fun before() {
    start()
  }

  override fun after() {
    server?.stop(1, 1)
  }
}
