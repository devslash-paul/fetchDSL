package net.devslash

import net.devslash.util.getCall
import net.devslash.util.requestDataFromList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class UrlProvidersTest {
  @Test
  fun testBasicProvider() {
    val url = "http://example.com"
    val provider = getUrlProvider(
      getCall(url = url), requestDataFromList(listOf())
    )
    assertEquals(url, provider.get())
  }

  @Test
  fun testReplacement() {
    val provider = getUrlProvider(
      getCall(url = "http://!1!"),
      requestDataFromList(listOf("example.com"))
    )
    assertEquals("http://example.com", provider.get())
  }
}
