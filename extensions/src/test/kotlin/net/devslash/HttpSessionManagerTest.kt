package net.devslash

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.runBlocking
import net.devslash.data.FileDataSupplier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTimeout
import org.junit.jupiter.api.Test
import java.time.Duration.ofSeconds
import java.util.concurrent.CountDownLatch

internal class HttpSessionManagerTest : ServerTest() {
  override lateinit var appEngine: ApplicationEngine

  @Test
  fun test302Redirect() {
    appEngine = embeddedServer(Netty, serverPort) {
      routing {
        get("/") {
          call.response.header("set-cookie", "session=abcd")
          call.response.status(HttpStatusCode.fromValue(302))
          call.respondText("Hi there")
        }
      }
    }
    start()

    var cookie: String? = null
    var body: String? = null
    runBlocking {
      runHttp {
        call(address) {
          output {
            +object : BasicOutput {
              override fun accept(resp: net.devslash.HttpResponse, data: RequestData) {
                cookie = resp.headers["set-cookie"]!![0]
                body = String(resp.body)
              }
            }
          }
        }
      }
    }

    assertEquals("session=abcd", cookie)
    assertEquals("Hi there", body)
  }

  @Test
  fun testMultiRequest() {
    appEngine = embeddedServer(Netty, serverPort) {
      routing {
        get("/") {
          call.respond("")
        }
      }
    }
    start()

    assertTimeout(ofSeconds(5)) {
      val countdown = CountDownLatch(15)
      val path = HttpSessionManagerTest::class.java.getResource("/testfile.log").path
      runHttp {
        concurrency = 15
        call(address) {
          postHook {
            +object : SimplePostHook {
              override fun accept(resp: net.devslash.HttpResponse) {
                countdown.countDown()
                countdown.await()
              }
            }
          }
          data = FileDataSupplier(name = path)
        }
      }
    }
  }
}
