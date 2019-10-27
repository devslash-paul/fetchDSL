package net.devslash

import io.ktor.client.response.HttpResponse
import io.mockk.coEvery
import io.mockk.mockk
import net.devslash.data.ListDataSupplier
import net.devslash.util.getResponseWithBody
import org.junit.Assert.assertTrue
import org.junit.Test

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
        data = ListDataSupplier(listOf(1, 2))
      }
    }.build()).run()

    val diff = times[1] - times[0]
    assertTrue(diff >= 30)
  }

}
