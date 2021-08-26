package net.devslash

import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import net.devslash.data.ListDataSupplier
import net.devslash.data.Repeat
import net.devslash.pipes.Pipe
import net.devslash.post.Filter
import net.devslash.post.LogResponse
import net.devslash.pre.LogRequest
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.collection.IsMapContaining
import org.junit.Assert.assertThrows
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import java.io.ByteArrayInputStream
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicInteger

class BasicIntegrationTest {

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
          post("/bounce") {
            bounceResponse()
          }
          get("/bounce") {
            bounceResponse()
          }
        }
      }
      server.start()
    }

    private suspend fun PipelineContext<Unit, ApplicationCall>.bounceResponse() {
      call.request.headers.filter { name, _ -> !listOf("content-length", "content-type").contains(name) }.forEach { key, list ->
        // Filter some
        list.forEach { call.response.headers.append(key, it) }
      }
      val message = call.receive<String>()
      call.respond(message)
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
          +Filter({ String(it.body) == "B" }) {
            +object : ResolvedFullDataAfterHook<List<String>> {
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
  fun testBeforeAfterActions() {
    var beforeHit = false
    var afterHit = false
    var bodyResult = "unset"

    runHttp {
      call("${address()}/bounce") {
        type = HttpMethod.GET
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
            bodyResult = String(resp.body)
          }
        }
      }
    }

    assertThat(beforeHit, equalTo(true))
    assertThat(afterHit, equalTo(true))
    assertThat(bodyResult, equalTo("Body"))
  }

  @Test
  fun testAccurateRequestsOver1kRequests() {
    val testNum = 1000
    val counted = AtomicInteger()
    runHttp {
      call<Unit>("${address()}/bounce") {
        data = Repeat(testNum)
        after {
          action {
            counted.getAndIncrement()
          }
        }
      }
    }
    assertThat(counted.get(), equalTo(testNum))
  }

  @Test
  fun testUrlProviderBasics() {
    var status = 0
    val urlData = "${address()}/bounce"
    runHttp {
      call({ _, d -> d.get()[0] }) {
        data = ListDataSupplier(listOf(urlData))
        after {
          action {
            status = resp.statusCode
          }
        }
      }
    }

    assertThat(status, equalTo(200))
  }

  @Test
  fun testBasicStringOverride() {
    var response = 0
    runHttp {
      call<String>({ _, d ->
        "${address()}/${d.get()}"
      }) {
        data = ListDataSupplier.typed(listOf("bounce"))
        before {
          +LogRequest()
        }
        after {
          action {
            response = resp.statusCode
          }
        }
      }
    }

    assertThat(response, equalTo(200))
  }

  @Test
  fun testReifiedTypeProviders() {
    var url = 0
    var beforeAction = 0
    var beforeObject = 0
    var afterAction = 0
    var afterObject = 0

    runHttp {
      call<Int>({ _, d ->
        url = d.get()
        "${address()}/bounce"
      }) {
        data = ListDataSupplier(listOf(1))
        before {
          action { beforeAction = data }
          +object : ResolvedSessionPersistingBeforeHook<Int> {
            override suspend fun accept(sessionManager: SessionManager, cookieJar: CookieJar, req: HttpRequest, data: Int) {
              beforeObject = data
            }
          }
        }
        after {
          action { afterAction = data }
          +object : ResolvedFullDataAfterHook<Int> {
            override fun accept(req: HttpRequest, resp: HttpResponse, data: Int) {
              afterObject = data
            }
          }
        }
      }
    }

    assertThat(url, equalTo(1))
    assertThat(beforeObject, equalTo(1))
    assertThat(beforeAction, equalTo(1))
    assertThat(afterObject, equalTo(1))
    assertThat(afterAction, equalTo(1))
  }

  @Test
  fun testRuntimeFailsReported() {
    assertThrows(DSLVisitorException::class.java) {
      runHttp {
        call("${address()}/bounce") {
          data = ListDataSupplier(listOf("Hi"))
          before {
            +object : SessionPersistingBeforeHook {
              override suspend fun accept(sessionManager: SessionManager, cookieJar: CookieJar, req: HttpRequest, data: RequestData<*>) {
                // should fail
                data.mustGet<Int>()
              }
            }
          }
        }
      }
    }
  }

  @Test
  fun testRuntimeFailsReportedInAfter() {
    assertThrows(DSLVisitorException::class.java) {
      runHttp {
        call("${address()}/bounce") {
          data = ListDataSupplier(listOf("Hi"))
          after {
            +object : FullDataAfterHook {
              override fun accept(req: HttpRequest, resp: HttpResponse, data: RequestData<*>) {
                data.mustGet<Int>()
              }
            }
          }
        }
      }
    }
  }

  @Test
  fun testHeadersSuccessfullyAdded() {
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

  @Test
  fun testStringListToTypedPipe() {
    val toGo = listOf("A", "bounce")
    val pipe = Pipe { resp, data ->
      listOf(ListRequestData(1))
    }
    var statusCode = 0
    runHttp {
      call("${address()}/bounce") {
        after {
          +pipe
        }
      }
      call<Int>({ _, data -> address() + "/" + toGo[data.get()] }) {
        data = pipe
        after {
          action {
            statusCode = resp.statusCode
          }
        }
      }
    }

    assertThat(statusCode, equalTo(200))
  }

  @Test
  fun testMultiSidedTypedPipe() {
    val pipe = Pipe<Int, Double> { _, _ -> listOf(ListRequestData(1.1)) }
    var secondData = 0.0
    runHttp {
      call<Int>("${address()}/bounce") {
        data = ListDataSupplier(listOf(1))
        after {
          +pipe
        }
      }
      call<Double>("${address()}/bounce") {
        data = pipe
        before {
          action {
            secondData = data
          }
        }
      }
    }

    assertThat(secondData, equalTo(1.1))
  }
}
