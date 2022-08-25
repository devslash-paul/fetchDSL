package net.devslash

import net.devslash.data.ListDataSupplier
import net.devslash.pipes.Pipe
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertThrows
import org.junit.ClassRule
import org.junit.Test

class TypedUsageIntegrationTest {

  companion object {
    @JvmField @ClassRule
    val testServer = TestServer()
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
        "${testServer.address()}/bounce"
      }) {
        data = ListDataSupplier(listOf(1))
        before {
          action { beforeAction = data }
          +object : ResolvedSessionPersistingBeforeHook<Int> {
            override suspend fun accept(subCallRunner: CallRunner<Int>, cookieJar: CookieJar, req: HttpRequest, data: Int) {
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
        call("${testServer.address()}/bounce") {
          data = ListDataSupplier(listOf("Hi"))
          before {
            +object : SessionPersistingBeforeHook {
              override suspend fun accept(subCallRunner: CallRunner<*>, cookieJar: CookieJar, req: HttpRequest, data: RequestData<*>) {
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
        call("${testServer.address()}/bounce") {
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
  fun testStringListToTypedPipe() {
    val toGo = listOf("A", "bounce")
    val pipe = Pipe { _, _ ->
      listOf(ListRequestData(1))
    }
    var statusCode = 0
    runHttp {
      call("${testServer.address()}/bounce") {
        after {
          +pipe
        }
      }
      call<Int>({ _, data -> testServer.address() + "/" + toGo[data.get()] }) {
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
      call<Int>("${testServer.address()}/bounce") {
        data = ListDataSupplier(listOf(1))
        after {
          +pipe
        }
      }
      call<Double>("${testServer.address()}/bounce") {
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
