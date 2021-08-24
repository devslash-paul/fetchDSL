package net.devslash

import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.ByteArrayInputStream
import java.net.ServerSocket

class TestIntegration {

  companion object {
    private lateinit var server: NettyApplicationEngine
    private var port: Int = 0
    val address = { "http://localhost:$port" }

    @BeforeClass
    @JvmStatic
    fun setup() {
      port = ServerSocket(0).localPort
      server = embeddedServer(Netty, port) {
        routing {
          get("/bounce") {
            call.respond(call.receiveText())
          }
        }
      }
      server.start()
    }
  }

  @Test
  fun testBasicHookIntegration() {
    println("${address()}/bounce")
    var beforeHit = false
    var afterHit = false
    var body = ""

    runHttp {
      call("${address()}/bounce") {
        body {
          rawValue = {
            ByteArrayInputStream("Body".toByteArray())
          }
        }
        before {
          action {
            beforeHit = true
          }
        }
        after {
          action {
            afterHit = true
            body = String(resp.body)
          }
        }
      }
    }

    assertThat(beforeHit, equalTo(true))
    assertThat(afterHit, equalTo(true))
    assertThat(body, equalTo("Body"))
  }

}
