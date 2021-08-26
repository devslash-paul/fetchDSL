package net.devslash

import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import net.devslash.data.ListDataSupplier
import net.devslash.outputs.DebugOutput
import net.devslash.post.Filter
import net.devslash.post.LogResponse
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.collection.IsMapContaining
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.ByteArrayInputStream
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicInteger

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
            call.request.headers.forEach { key, list ->
              list.forEach { call.response.headers.append(key, it) }
            }
            call.respond(call.receiveText())
          }
        }
      }
      server.start()
    }
  }

  @Test
  fun testBasicFilter() {
    val arrived = AtomicInteger()
    runHttp {
      call("${address()}/bounce") {
        data = ListDataSupplier(listOf("A", "B"))
        body {
          value("!1!", indexValueMapper)
        }
        after {
          +Filter({String(it.body) == "B"}) {
            +object: ResolvedFullDataAfterHook<List<String>> {
              override fun accept(req: HttpRequest, resp: HttpResponse, data: List<String>) {
                arrived.getAndIncrement()
              }
            }
          }
        }
      }
    }
    assertThat(arrived.get(), equalTo(1))
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

  @Test
  fun testHeaderSet() {
    var responseHeaders = mapOf<String, List<String>>()
    val testHeaders = mapOf("test" to listOf("value"))
    runHttp {
      call("${address()}/bounce") {
        headers = testHeaders
        after {
          action {
            responseHeaders = resp.headers
          }
        }
      }
    }

    assertThat(responseHeaders, IsMapContaining.hasEntry(equalTo("test"), equalTo(listOf("value"))))
  }

}
