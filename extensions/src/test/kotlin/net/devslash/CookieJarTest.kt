package net.devslash

import net.devslash.util.requestDataFromList
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual.equalTo
import org.junit.jupiter.api.Test
import java.net.URL

internal class CookieJarTest {

  private val jar: CookieJar = CookieJar()
  private val request = HttpRequest(HttpMethod.GET, "", "")

  @Test
  fun testSingleCookieSet() {
    jar.accept(responseWithHeaders(mapOf("Set-Cookie" to listOf("A=B"))))
    jar.accept(request, requestDataFromList(listOf()))

    assertThat(request.headers["Cookie"], equalTo(listOf("A=B")))
  }

  @Test fun testMultipleCaseCookieSet() {
    jar.accept(
        responseWithHeaders(mapOf("set-Cookie" to listOf("A=B"), "SET-COOKIE" to listOf("C=D"))))
    jar.accept(request, requestDataFromList(listOf()))


    assertThat(request.headers["Cookie"], equalTo(listOf("A=B; C=D")))
  }

  @Test fun testSetMultipleOfTheSameKey() {
    jar.accept(
        responseWithHeaders(mapOf("set-Cookie" to listOf("A=B"), "SET-COOKIE" to listOf("A=D"))))
    jar.accept(request, requestDataFromList(listOf()))


    assertThat(request.headers["Cookie"], equalTo(listOf("A=D")))
  }

  private fun responseWithHeaders(headers: Map<String, List<String>>) = HttpResponse(
      URL("https://example"), 200, headers, "".toByteArray())
}
