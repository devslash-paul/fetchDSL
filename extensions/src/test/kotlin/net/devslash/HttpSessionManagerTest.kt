package net.devslash

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockserver.junit.MockServerRule
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch

internal class HttpSessionManagerTest {
  @get:Rule
  val mockServerRule: MockServerRule = MockServerRule(this)

  @Test
  fun test302Redirect() {
    val client = mockServerRule.client
    client.`when`(HttpRequest.request())
        .respond(HttpResponse.response().withStatusCode(302)
            .withBody("Hi there")
            .withCookie("session", "abcd"))
    val address = client.remoteAddress()

    var cookie: String? = null
    var body: String? = null
    runBlocking {
      runHttp {
        call(getAddress(address)) {
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
    val client = mockServerRule.client
    client.`when`(HttpRequest.request()).respond(HttpResponse.response())
    val address = client.remoteAddress()
    val countdown = CountDownLatch(30)
    val path = HttpSessionManagerTest::class.java.getResource("/testfile.log").path
    runHttp {
      concurrency = 30
      call(getAddress(address)) {
        postHook {
          +object : SimplePostHook {
            override fun accept(resp: net.devslash.HttpResponse) {
              countdown.countDown()
              countdown.await()
            }
          }
        }
        data {
          file {
            name = path
          }
        }
      }
    }
  }

  private fun getAddress(address: InetSocketAddress) = "http://" + address.hostString + ":" + address.port
}
