package net.devslash.pipes

import net.devslash.HttpResponse
import net.devslash.util.requestDataFromList
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.net.URL

internal class ResettablePipeTest {

  @Test
  fun testPipeStartsEmpty() {
    val pipe = ResettablePipe({ _, _ -> listOf("A", "B") }, null)

    assertThat(pipe.hasNext(), equalTo(false))
  }

  @Test
  fun testPipeSingleCase() {
    val pipe = ResettablePipe({ r, _ -> listOf(String(r.body)) }, null)

    pipe.accept(
        HttpResponse(URL("http://"), 200, mapOf(), "result".toByteArray()),
      requestDataFromList(listOf())
    )

    assertThat(pipe.hasNext(), equalTo(true))
    val data = pipe.getDataForRequest()
    assertThat(data, not(nullValue()))
    assertThat(data.getReplacements()["!1!"], equalTo("result"))
    assertThat(pipe.hasNext(), equalTo(false))

    // now reset
    pipe.reset()
    assertThat(pipe.hasNext(), equalTo(true))
    val nextData = pipe.getDataForRequest()
    assertThat(data, equalTo(nextData))
  }

  @Test
  fun testPipeCanReturnMultipleResults() {
    val vals = listOf("a", "b", "c")
    val pipe = Pipe({ _, _ -> vals }, " ")
    pipe.accept(
        HttpResponse(URL("http://"), 200, mapOf(), byteArrayOf()),
      requestDataFromList(listOf())
    )

    vals.forEach {
      assertThat(pipe.hasNext(), equalTo(true))
      assertThat(pipe.getDataForRequest().getReplacements(), equalTo(mapOf("!1!" to it)))
    }
  }

  @Test
  fun testPipeAcceptsMultipleAndReturnsInOrder() {
    val pipe = Pipe({ r, _ -> listOf(String(r.body)) }, " ")
    pipe.accept(
        HttpResponse(URL("http://"), 200, mapOf(), "a".toByteArray()),
      requestDataFromList(listOf())
    )
    pipe.accept(
        HttpResponse(URL("http://"), 200, mapOf(), "b".toByteArray()),
      requestDataFromList(listOf())
    )
    pipe.accept(
        HttpResponse(URL("http://"), 200, mapOf(), "c".toByteArray()),
      requestDataFromList(listOf())
    )

    val values = listOf("a", "b", "c")
    values.forEach {
      assertThat(pipe.hasNext(), equalTo(true))
      val data = pipe.getDataForRequest()
      val repl = data.getReplacements()
      assertThat(repl, equalTo(mapOf("!1!" to it)))
    }

    assertThat(pipe.hasNext(), equalTo(false))
  }
}
