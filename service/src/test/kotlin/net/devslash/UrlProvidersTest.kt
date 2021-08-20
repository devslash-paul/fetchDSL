package net.devslash

import net.devslash.util.getTypedCall
import net.devslash.util.requestDataFromList
import org.junit.Assert.assertEquals
import org.junit.Test

internal class UrlProvidersTest {
  @Test
  fun testBasicProvider() {
    val url = "http://example.com"
    val provider = getUrlProvider(
        getTypedCall<List<String>>(url = url)
    )
    assertEquals(url, provider(url, requestDataFromList(listOf())))
  }

  @Test
  fun testReplacement() {
    val url = "http://!1!"
    val provider = getUrlProvider(
        getTypedCall<List<String>>(url = url),
    )
    val data = requestDataFromList(listOf("example.com"))
    assertEquals("http://example.com", provider(url, data))
  }

  @Test
  fun testGenericTypeReplacement() {
    data class T(val s: String)

    val t = T("Expected")
    val builder = CallBuilder<T>("")
    builder.urlProvider = { _, b ->
      b.value().s
    }

    val urlProvider = getUrlProvider(builder.build())
    assertEquals("Expected", urlProvider("a", ListRequestData(t)))
  }
}
