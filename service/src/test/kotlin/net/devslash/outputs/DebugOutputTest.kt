package net.devslash.outputs

import net.devslash.ListRequestData
import net.devslash.util.getResponseWithBody
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

internal class DebugOutputTest {
  @Test
  fun testBasic() {
    val out = DebugOutput().accept(
      getResponseWithBody("abc".toByteArray()), ListRequestData(listOf("a", "b", "c"))
    )

    assertThat(
      String(out!!), equalTo(
        """
           ----------------
           url: http://example.com
           status: 200
           headers: [{}]
           data: [a, b, c]
           body ->
           abc
           ----------------
        """.trimIndent()
      )
    );
  }
}
