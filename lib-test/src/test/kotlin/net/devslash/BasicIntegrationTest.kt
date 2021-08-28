package net.devslash

import net.devslash.data.ListDataSupplier
import net.devslash.data.Repeat
import net.devslash.post.Filter
import net.devslash.pre.LogRequest
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.lessThan
import org.hamcrest.collection.IsMapContaining
import org.junit.ClassRule
import org.junit.Test
import java.io.ByteArrayInputStream
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

class BasicIntegrationTest {

  companion object {
    @ClassRule @JvmField
    val testServer: TestServer = TestServer()
  }

  @Test
  fun testBasicFilter() {
    val arrived = AtomicInteger()
    runHttp {
      call("${testServer.address()}/bounce") {
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
  fun testRateLimitIsReasonable() {
    val result = measureTimeMillis {
      runHttp {
        rateLimit(10, Duration.ofSeconds(1))
        call<Unit>("${testServer.address()}/bounce") {
          data = Repeat(50)
        }
      }
    }
    assertThat(result, greaterThan(5000))
    assertThat(result, lessThan(7000))
  }

  @Test
  fun testBeforeAfterActions() {
    var beforeHit = false
    var afterHit = false
    var bodyResult = "unset"

    runHttp {
      call("${testServer.address()}/bounce") {
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
      call<Unit>("${testServer.address()}/bounce") {
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
    val urlData = "${testServer.address()}/bounce"
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
        "${testServer.address()}/${d.get()}"
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
  fun testHeadersSuccessfullyAdded() {
    var responseHeaders = mapOf<String, List<String>>()
    val testHeaders = mapOf("test" to listOf("value"))
    runHttp {
      call("${testServer.address()}/bounce") {
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
