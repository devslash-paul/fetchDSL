package net.devslash

import net.devslash.util.getCall
import org.junit.Assert.assertEquals
import org.junit.Test

internal class BodyProviderTest {

  @Test fun testEmpty() {
    val provider = getBodyProvider(
      getCall(), ListBasedRequestData(listOf("b", "d"))
    )

    assertEquals(EmptyBodyProvider::class, provider::class)
  }

  @Test
  fun testWithBody() {
    val provider = getBodyProvider(
      getCall(HttpBody(null, mapOf("a" to listOf("b"), "c" to listOf("d")), null, null)), ListBasedRequestData<String>(
        listOf())
    )

    assertEquals(mapOf("a" to listOf("b"), "c" to listOf("d")), (provider as FormBody<String>).get())
  }

  @Test fun testBodyWithReplaceableValues() {
    val provider = getBodyProvider(
      getCall(HttpBody("a=!1!&c=!2!", null, null, null)), ListBasedRequestData(listOf("b", "d"))
    )
    assertEquals("a=b&c=d", (provider as BasicBodyProvider<String>).get())
  }

  @Test fun testParamsWithReplacement() {
    val provider = getBodyProvider(
      getCall(HttpBody(null, mapOf("a" to listOf("!1!"), "c" to listOf("!2!")), null, null)),
      ListBasedRequestData(listOf("b", "d"))
    )

    assertEquals(mapOf("a" to listOf("b"), "c" to listOf("d")), (provider as FormBody<String>).get())
  }
}

