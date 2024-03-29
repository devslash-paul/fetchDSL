package net.devslash.outputs

import net.devslash.HttpResponse
import net.devslash.ListRequestData
import net.devslash.OutputFormat
import net.devslash.RequestData
import net.devslash.util.basicRequest
import net.devslash.util.getResponseWithBody
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

internal class StdOutTest {

  @Test
  fun testBasicStdoutCopiesBody() {
    val byteStream = ByteArrayOutputStream()
    val out = StdOut(PrintStream(byteStream))

    assertOutputMatches(out, byteStream, body)
  }

  @Test
  fun testOverrideFormatUsed() {
    val byteStream = ByteArrayOutputStream()
    val pattern = "test pattern"
    val out = StdOut(PrintStream(byteStream), object : OutputFormat {
      override fun accept(resp: HttpResponse, data: RequestData<*>): ByteArray {
        return pattern.toByteArray()
      }
    })

    assertOutputMatches(out, byteStream, pattern)
  }

  @Test
  fun testNullOutputValid() {
    val byteStream = ByteArrayOutputStream()
    val out = StdOut(PrintStream(byteStream), object : OutputFormat {
      override fun accept(resp: HttpResponse, data: RequestData<*>): ByteArray? = null
    })

    assertOutputMatches(out, byteStream, "")
  }

  private fun assertOutputMatches(out: StdOut, byteStream: ByteArrayOutputStream, actual: String) {
    out.accept(
      basicRequest(), getResponseWithBody(body.toByteArray()),
      ListRequestData(listOf<String>())
    )

    assertThat(
      actual,
      equalTo(String(byteStream.toByteArray()).trim())
    )
  }

  companion object {
    const val body = "Test\nResponse"
  }
}
