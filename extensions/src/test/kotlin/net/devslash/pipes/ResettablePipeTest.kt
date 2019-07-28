package net.devslash.pipes

import kotlinx.coroutines.runBlocking
import net.devslash.HttpResponse
import net.devslash.util.getBasicRequest
import net.devslash.util.requestDataFromList
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import java.net.URL

internal class ResettablePipeTest {

  @Test
  fun testPipeStartsEmpty() = runBlocking{
    val pipe = ResettablePipe({ _, _ -> listOf("A", "B") }, null)

    assertThat(pipe.getDataForRequest(), nullValue())
  }

  @Test
  fun testPipeSingleCase() = runBlocking {
    val pipe = ResettablePipe({ r, _ -> listOf(String(r.body)) }, null)

    pipe.accept(
        getBasicRequest(),
        HttpResponse(URL("http://"), 200, mapOf(), "result".toByteArray()),
      requestDataFromList(listOf())
    )

    val data = pipe.getDataForRequest()!!
    assertThat(data, not(nullValue()))
    assertThat(data.getReplacements()["!1!"], equalTo("result"))
    assertThat(pipe.getDataForRequest(), nullValue())

    // now reset
    pipe.reset()
    val nextData = pipe.getDataForRequest()!!
    assertThat(data, equalTo(nextData))
  }

  @Test
  fun testPipeCanReturnMultipleResults() = runBlocking {
    val vals = listOf("a", "b", "c")
    val pipe = Pipe({ _, _ -> vals }, " ")
    pipe.accept(
        getBasicRequest(),
        HttpResponse(URL("http://"), 200, mapOf(), byteArrayOf()),
      requestDataFromList(listOf())
    )

    vals.forEach {
      assertThat(pipe.getDataForRequest()!!.getReplacements(), equalTo(mapOf("!1!" to it)))
    }
  }

  @Test
  fun testPipeAcceptsMultipleAndReturnsInOrder() = runBlocking {
    val pipe = Pipe({ r, _ -> listOf(String(r.body)) }, " ")
    pipe.accept(
        getBasicRequest(),
        HttpResponse(URL("http://"), 200, mapOf(), "a".toByteArray()),
      requestDataFromList(listOf())
    )
    pipe.accept(
        getBasicRequest(),
        HttpResponse(URL("http://"), 200, mapOf(), "b".toByteArray()),
      requestDataFromList(listOf())
    )
    pipe.accept(
        getBasicRequest(),
        HttpResponse(URL("http://"), 200, mapOf(), "c".toByteArray()),
      requestDataFromList(listOf())
    )

    val values = listOf("a", "b", "c")
    values.forEach {
      val data = pipe.getDataForRequest()!!
      val repl = data.getReplacements()
      assertThat(repl, equalTo(mapOf("!1!" to it)))
    }

    assertThat(pipe.getDataForRequest(), nullValue())
  }
}
