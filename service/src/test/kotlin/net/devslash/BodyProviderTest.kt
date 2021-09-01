package net.devslash

import net.devslash.util.getCall
import org.junit.Assert.assertEquals
import org.junit.Test

internal class BodyProviderTest {

  @Test
  fun testEmpty() {
    val provider = getBodyProvider(
        getCall(), listOf("b", "d")
    )

    assertEquals(EmptyBody::class, provider::class)
  }

  @Test
  fun testWithBody() {
    val provider = getBodyProvider(
        getCall(
            HttpBody(
                null,
                null,
                null,
                { mapOf("a" to listOf("b"), "c" to listOf("d")) },
                null,
                null,
                null,
                null
            )
        ),
        listOf()
    )

    assertEquals(mapOf("a" to listOf("b"), "c" to listOf("d")), (provider as FormRequestBody).form)
  }

  @Test
  fun testBodyWithReplaceableValues() {
    val provider = getBodyProvider(
        getCall(HttpBody("a=!1!&c=!2!", indexValueMapper, null, null, null, null, null, null)),
        listOf("b", "d")
    )
    assertEquals("a=b&c=d", (provider as StringRequestBody).body)
  }

  @Test
  fun testParamsWithReplacement() {
    val provider = getBodyProvider(
        getCall(
            HttpBody(
                null,
                null,
                null,
                { formIndexed(mapOf("a" to listOf("!1!"), "c" to listOf("!2!")), data) },
                null,
                null,
                null,
                null
            )
        ),
        listOf("b", "d")
    )

    assertEquals(mapOf("a" to listOf("b"), "c" to listOf("d")), (provider as FormRequestBody).form)
  }
}

