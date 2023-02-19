package net.devslash

import net.devslash.util.requestDataFromList
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual.equalTo
import org.junit.Test
import java.net.URI
import java.time.Duration
import java.time.Instant

internal class DefaultCookieJarTest {

  private val jar: CookieJar = DefaultCookieJar()
  private val standardRequest = HttpRequest(HttpMethod.GET, "https://example.com", EmptyBody)

  @Test
  fun testSingleCookieSet() {
    jar.accept(responseWithHeaders(mapOf("Set-Cookie" to listOf("A=B"))))
    jar.accept(standardRequest, requestDataFromList(listOf()))

    assertThat(standardRequest.headers["Cookie"], equalTo(listOf("A=B")))
  }

  @Test
  fun testMultipleCaseCookieSet() {
    jar.accept(
        responseWithHeaders(mapOf("set-Cookie" to listOf("A=B"), "SET-COOKIE" to listOf("C=D")))
    )
    jar.accept(standardRequest, requestDataFromList(listOf()))


    assertThat(standardRequest.headers["Cookie"], equalTo(listOf("A=B; C=D")))
  }

  @Test
  fun testSetMultipleOfTheSameKey() {
    jar.accept(
        responseWithHeaders(mapOf("set-Cookie" to listOf("A=B"), "SET-COOKIE" to listOf("A=D")))
    )
    jar.accept(standardRequest, requestDataFromList(listOf()))


    assertThat(standardRequest.headers["Cookie"], equalTo(listOf("A=D")))
  }

  @Test
  fun testDomainsDoNotShareCookies() {
    jar.accept(responseWithHeaders(mapOf("Set-Cookie" to listOf("A=B"))))

    val otherSizeRequest = HttpRequest(HttpMethod.GET, "https://differentDomain.com", EmptyBody)
    jar.accept(otherSizeRequest, requestDataFromList(listOf()))
    jar.accept(standardRequest, requestDataFromList(listOf()))

    assertThat(otherSizeRequest.headers["Cookie"], `is`(nullValue()))
    assertThat(standardRequest.headers["Cookie"], equalTo(listOf("A=B")))
  }

  @Test
  fun testProtocolsDoNotShareCookies() {
    jar.accept(responseWithHeaders(mapOf("Set-Cookie" to listOf("A=B"))))
    val httpRequest = HttpRequest(HttpMethod.GET, "http://example.com", EmptyBody)

    jar.accept(httpRequest, requestDataFromList(listOf()))
    jar.accept(standardRequest, requestDataFromList(listOf("A=B")))
  }

  private fun responseWithHeaders(
      headers: Map<String, List<String>>,
      url: String = "https://example.com/test"
  ) = HttpResponse(
      URI(url),
      200,
      headers,
      "".toByteArray()
  )
}
