package net.devslash

import com.github.kittinunf.fuel.core.Method
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual.equalTo
import org.junit.Before
import org.junit.Test
import java.net.URL

internal class CookieJarTest {

  private lateinit var jar: CookieJar
  private lateinit var request: HttpRequest

  @Before fun setup() {
    jar = CookieJar()
    request = HttpRequest(Method.GET, "", "")
  }

  @Test fun testSingleCookieSet() {
    jar.accept(responseWithHeaders(mapOf("Set-Cookie" to listOf("A=B"))))
    jar.accept(request, requestDataFromList(listOf()))

    assertThat(request.headers, equalTo(mutableMapOf("Cookie" to "A=B")))
  }

  @Test fun testMultipleCaseCookieSet() {
    jar.accept(
        responseWithHeaders(mapOf("set-Cookie" to listOf("A=B"), "SET-COOKIE" to listOf("C=D"))))
    jar.accept(request, requestDataFromList(listOf()))


    assertThat(request.headers, equalTo(mutableMapOf("Cookie" to "A=B; C=D")))
  }

  @Test fun testSetMultipleOfTheSameKey() {
    jar.accept(
        responseWithHeaders(mapOf("set-Cookie" to listOf("A=B"), "SET-COOKIE" to listOf("A=D"))))
    jar.accept(request, requestDataFromList(listOf()))


    assertThat(request.headers, equalTo(mutableMapOf("Cookie" to "A=D")))
  }

  private fun responseWithHeaders(headers: Map<String, List<String>>) = HttpResponse(
      URL("https://example"), 200, headers, "".toByteArray())
}
