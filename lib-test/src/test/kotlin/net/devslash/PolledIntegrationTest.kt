package net.devslash

import net.devslash.outputs.DebugOutput
import net.devslash.post.LogResponse
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.ClassRule
import org.junit.Test

class PolledIntegrationTest {
  companion object {
    @JvmField
    @ClassRule
    val testServer = TestServer()
  }

  @Test(timeout = 5000)
  fun testHappyPathForCountedPolling() {
    var count = testServer.count.get()
    val end = count + 10
    runHttp {
      call("${testServer.address()}/count") {
        install(PollUntil { String(resp.body) == "$end" })
        after {
          action {
            count += 1
          }
        }
      }
    }
    assertThat(count, equalTo(end))
  }

  @Test(timeout = 5000)
  fun testPollingWithBodyContinuesSupplyingSameBody() {
    var count = testServer.count.get()
    val end = count + 10
    runHttp {
      call("${testServer.address()}/count") {
        install(PollUntil({ String(resp.body) == "$end" }, listOf("A")))
        body {
          value("!1!")
        }
        after {
          action {
            count += 1
            assertThat((req.body as StringRequestBody).body, equalTo("A"))
          }
        }
      }
    }
    assertThat(count, equalTo(end))
  }

}
