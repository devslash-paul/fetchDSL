package net.devslash

import io.ktor.client.response.HttpResponse
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger

fun getResponseWithBody(body: ByteArray): net.devslash.HttpResponse {
  return HttpResponse(URL("http://example.com"), 200, mapOf(), body)
}

class DelayTest {

  @Test
  fun testDelayCausesWaitBetweenCalls() {
    val engine = mockk<HttpDriver>(relaxed = true)
    val resp = mockk<HttpResponse>()
    val times = mutableListOf<Long>()

    coEvery { engine.mapResponse(any()) } returns getResponseWithBody(ByteArray(0))
    coEvery { engine.call(any()) } answers {
      times.add(System.currentTimeMillis())
      Success(resp)
    }

    HttpSessionManager(engine, SessionBuilder().apply {
      delay = 30
      call("http://example.org") {
        data = object : RequestDataSupplier<List<String>> {
          val count = AtomicInteger(0)
          override suspend fun getDataForRequest(): RequestData<List<String>>? {
            if (count.incrementAndGet() > 2) {
              return null
            }
            return ListBasedRequestData(listOf())
          }

        }
      }
    }.build()).run()

    val diff = times[1] - times[0]
    assertTrue(diff >= 30)
  }

}
