package net.devslash.pipes

import kotlinx.coroutines.runBlocking
import net.devslash.HttpResponse
import net.devslash.ListRequestData
import net.devslash.mustGet
import net.devslash.util.basicRequest
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.net.URI

internal class PipeTest {

  @Test
  fun testPipeStartsEmpty() = runBlocking {
    val pipe = Pipe { _, _ -> listOf(ListRequestData(listOf("A", "B"))) }

    assertThat(pipe.getDataForRequest(), nullValue())
  }

  @Test
  fun testPipeSingleCase() = runBlocking {
    val pipe = Pipe { r, _ -> listOf(ListRequestData(listOf(String(r.body)))) }

    pipe.accept(
        basicRequest(),
        HttpResponse(URI("http://a"), 200, mapOf(), "result".toByteArray()),
        listOf()
    )

    val data = pipe.getDataForRequest()!!
    assertThat(data, not(nullValue()))
    assertThat(data.mustGet<List<String>>()[0], equalTo("result"))
    assertThat(pipe.getDataForRequest(), nullValue())
  }

  @Test
  fun testPipeCanReturnMultipleResults() = runBlocking {
    val vals = listOf("a", "b", "c")
    val pipe = Pipe { _, _ -> vals.map { ListRequestData(listOf(it)) } }
    pipe.accept(
        basicRequest(),
        HttpResponse(URI("http://a"), 200, mapOf(), byteArrayOf()),
        listOf()
    )

    vals.forEach {
      assertThat(pipe.getDataForRequest()!!.mustGet<List<String>>()[0], equalTo(it))
    }
  }

  @Test
  fun testPipeAcceptsMultipleAndReturnsInOrder() = runBlocking {
    val pipe = Pipe { r, _ -> listOf(ListRequestData(listOf(String(r.body)))) }
    pipe.accept(
        basicRequest(),
        HttpResponse(URI("http://a"), 200, mapOf(), "a".toByteArray()),
        listOf()
    )
    pipe.accept(
        basicRequest(),
        HttpResponse(URI("http://a"), 200, mapOf(), "b".toByteArray()),
        listOf()
    )
    pipe.accept(
        basicRequest(),
        HttpResponse(URI("http://a"), 200, mapOf(), "c".toByteArray()),
        listOf()
    )

    val values = listOf("a", "b", "c")
    values.forEach {
      val data = pipe.getDataForRequest()!!
      val repl = data.mustGet<List<String>>()
      assertThat(repl[0], equalTo(it))
    }

    assertThat(pipe.getDataForRequest(), nullValue())
  }
}
