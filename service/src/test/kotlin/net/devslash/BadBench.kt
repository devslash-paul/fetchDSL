package net.devslash

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockHttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import org.junit.jupiter.api.Test
import java.util.concurrent.Semaphore
import kotlin.system.measureTimeMillis

class BadBench {

  @Test
  fun testBasicBenchmark() {
    var count = 0
    val semaphore = Semaphore(20)
    val engine: HttpClientEngine = MockEngine {

      delay(100)
      MockHttpResponse(call, HttpStatusCode.OK)
    }

    val time = measureTimeMillis {
      runHttp(engine) {
        concurrency = 100
        call("http://example.com") {
          preHook {}
          postHook {
            +({
              count++
              println(count)
            }).toPostHook()
          }
          data = object : RequestDataSupplier {
            override fun getDataForRequest(): RequestData {
              return ListBasedRequestData(emptyList())
            }

            override fun hasNext(): Boolean = count < 2000
          }
        }
      }
    }
    println(time)
  }
}

