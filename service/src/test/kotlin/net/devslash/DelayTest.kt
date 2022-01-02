package net.devslash

import net.devslash.data.ListDataSupplier
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI

class DelayTest {
  class TimingDriver(private val times: MutableList<Long>) : Driver {
    override suspend fun call(req: HttpRequest): HttpResult<HttpResponse, Exception> {
      times.add(System.currentTimeMillis())
      return Success(HttpResponse(URI("https://a"), 200, mapOf(), ByteArray(0)))
    }

    override fun close() {
    }
  }

  @Test
  fun testDelayCausesWaitBetweenCalls() {
    val times = mutableListOf<Long>()
    val engine = TimingDriver(times)
    val mgr = HttpSessionManager(engine)
    mgr.run(SessionBuilder().apply {
      delay = 30
      call("http://example.org") {
        data = ListDataSupplier(listOf(listOf("1"), listOf("2")))
      }
    }.build())

    val diff = times[1] - times[0]
    assertTrue(diff >= 30)
  }
}
