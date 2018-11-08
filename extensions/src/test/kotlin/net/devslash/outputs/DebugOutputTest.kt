package net.devslash.outputs

import net.devslash.ListBasedRequestData
import net.devslash.util.getResponseWithBody
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

internal class DebugOutputTest {
  @Test
  fun testBasic() {
    val out = DebugOutput().accept(
      getResponseWithBody("abc".toByteArray()), ListBasedRequestData(listOf("a", "b", "c"))
    )

    assertThat(
      String(out!!), equalTo(
        """
           ----------------
           url: http://example.com
           status: 200
           headers: [{}]
           data: {!1!=a, !2!=b, !3!=c}
           body ->
           abc
           ----------------
        """.trimIndent()
      )
    );
  }
}
