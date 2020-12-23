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
import kotlinx.coroutines.debug.junit4.CoroutinesTimeout
import kotlinx.coroutines.runBlocking
import net.devslash.data.FileDataSupplier
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch

internal class HttpSessionManagerTest : ServerTest() {

  @Rule
  @JvmField
  public val rule = CoroutinesTimeout(5000)

  override lateinit var appEngine: ApplicationEngine

  @Test
  fun test302Redirect() {
    appEngine = embeddedServer(Netty, serverPort) {
      routing {
        get("/") {
          call.response.header("set-cookie", "session=abcd")
          call.response.header("Location", "invalid")
          call.response.status(HttpStatusCode.fromValue(302))
          call.respondText("Hi there")
        }
      }
    }
    start()

    var cookie: String? = null
    var body: String? = null
    runBlocking {
      runHttp({}) {
        call(address) {
          after {
            +object : BasicOutput {
              override fun <T> accept(req: HttpRequest, resp: HttpResponse, data: RequestData<T>) {
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
          // this cannot be the empty string. That turns out to stuff up the blocking on response code.
          call.respond("A")
        }
      }
    }
    start()
    val testConcurrency = 2

    val countdown = CountDownLatch(testConcurrency)
    val path = HttpSessionManagerTest::class.java.getResource("/testfile.log").path
    runHttp {
      concurrency = testConcurrency
      call(address) {
        after {
          +object : SimpleAfterHook {
            override fun accept(resp: HttpResponse) {
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
