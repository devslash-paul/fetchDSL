package net.devslash

import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import org.junit.rules.ExternalResource

class TestServer : ExternalResource() {
  private var server: NettyApplicationEngine? = null
  private var port: Int = 0
  val address = { "http://localhost:${port}" }

  private fun start() {
    port = 50000
    println("USING PORT $port")
    server = embeddedServer(Netty, port) {
      routing {
        post("/form") {
          bounceForm()
        }
        post("/bounce") {
          bounceResponse()
        }
        get("/bounce") {
          bounceResponse()
        }
      }
    }
    server!!.start()
  }

  private suspend fun PipelineContext<Unit, ApplicationCall>.bounceForm() {
    try {
      call.request.headers.filter { name, _ -> !listOf("content-length", "content-type").contains(name.lowercase()) }.forEach { key, list ->
        // Filter some
        list.forEach { call.response.headers.append(key, it) }
      }
      val message = call.receiveParameters()
      var resp = ""
      message.forEach { key, list -> resp += key + ":[" + list.joinToString(",") + "]" }
      call.respond(resp)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private suspend fun PipelineContext<Unit, ApplicationCall>.bounceResponse() {
    call.request.headers.filter { name, _ -> !listOf("content-length", "content-type").contains(name.lowercase()) }.forEach { key, list ->
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
