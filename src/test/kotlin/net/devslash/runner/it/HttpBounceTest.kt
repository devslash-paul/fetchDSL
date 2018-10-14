package net.devslash.runner.it

import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import net.devslash.HttpMethod
import net.devslash.asReplaceableValue
import net.devslash.runner.pre.SkipIf
import net.devslash.runner.runHttp
import org.junit.Rule
import org.junit.Test
import org.mockserver.junit.MockServerRule
import org.mockserver.model.HttpRequest.request
import java.net.InetSocketAddress
import java.util.function.Predicate

class HttpBounceTest {
  @get:Rule val mockServerRule: MockServerRule = MockServerRule(this)

  @Test fun testBasicStep() {
    val client = mockServerRule.client
    val address = client.remoteAddress()

    runBlocking {
      runHttp {
        call(getAddress(address) + "/testPath")
      }.joinAll()
    }

    client.verify(request().withPath("/testPath"))
  }

  @Test fun testWithBodyParams() {
    val client = mockServerRule.client
    val address = client.remoteAddress()

    runBlocking {
      runHttp {
        call(getAddress(address)) {
          type = HttpMethod.POST
          headers = listOf("A" to "B".asReplaceableValue())
          body {
            value = "TestBody"
          }
        }
      }
    }

    client.verify(request().withBody("TestBody").withHeader("A", "B"))
  }

  @Test fun testPredicateSkipsRequest() {
    val client = mockServerRule.client
    val address = client.remoteAddress()

    runBlocking {
      runHttp {
        call(getAddress(address)) {
          preHook {
            +SkipIf(Predicate { true })
          }
        }
      }
    }

    client.verifyZeroInteractions()
  }


 private fun getAddress(address: InetSocketAddress) = "http://" + address.hostString + ":" + address.port
}
