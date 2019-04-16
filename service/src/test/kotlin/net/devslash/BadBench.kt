package net.devslash

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockHttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import org.junit.jupiter.api.Test
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

class BadBench {

  @Test
  fun testBasicBenchmark() {
    var count = AtomicInteger(0)
    val engine: HttpClientEngine = MockEngine {

      delay(100)
      MockHttpResponse(call, HttpStatusCode.OK)
    }

    val time = measureTimeMillis {
      runHttp(engine) {
        concurrency = 1000
        call("http://example.com/") {
          before {}
          after {
            +object: FullDataAfterHook {
              override fun accept(req: HttpRequest, resp: HttpResponse, data: RequestData) {
                println("!1!".asReplaceableValue().get(data))
              }
            }
          }
          data = object : RequestDataSupplier {
            override fun getDataForRequest(): RequestData {
              return ListBasedRequestData(listOf("" + count.incrementAndGet()))
            }

            override fun hasNext(): Boolean = count.get() < 2000
          }
        }
      }
    }
    println(time)
  }
}

