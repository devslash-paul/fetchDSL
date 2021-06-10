package net.devslash

import net.devslash.util.getCall
import net.devslash.util.requestDataFromList
import org.junit.Assert.assertEquals
import org.junit.Test

internal class UrlProvidersTest {
  @Test
  fun testBasicProvider() {
    val url = "http://example.com"
    val provider = getUrlProvider(
      getCall(url = url)
    )
    assertEquals(url, provider(url, requestDataFromList(listOf())))
  }

  @Test
  fun testReplacement() {
    val url = "http://!1!"
    val provider = getUrlProvider(
      getCall(url = url),
    )
    val data = requestDataFromList(listOf("example.com"))
    assertEquals("http://example.com", provider(url, data))
  }
}
