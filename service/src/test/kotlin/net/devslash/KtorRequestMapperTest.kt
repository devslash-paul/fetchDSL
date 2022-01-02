package net.devslash

import io.ktor.client.utils.*
import io.ktor.content.*
import io.ktor.http.*
import io.ktor.http.HttpMethod
import net.devslash.util.basicRequest
import net.devslash.util.basicUrl
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

internal class KtorRequestMapperTest {

  @Test
  fun testHttpMapBasicAttribute() {
    val httpRequest = basicRequest()
    val req = KtorRequestMapper.mapHttpToKtor(httpRequest)

    assertThat(req.url.buildString(), equalTo(basicUrl))
    assertThat(req.method, equalTo(HttpMethod.Get))
    assertThat(req.headers.build(), equalTo(Headers.Empty))
    assertThat(req.body, equalTo(EmptyContent))
  }

  @Test
  fun testHttpMapComplexAttributes() {
    val headers = mapOf(
        "header1" to listOf("value1", "value2"),
        "header2" to listOf("second1", "second2")
    )
    val builders = HeadersBuilder()
    headers.forEach {
      it.value.forEach { v -> builders.append(it.key, v) }
    }
    val obj = mapOf("string" to "value")
    val httpRequest = HttpRequest(net.devslash.HttpMethod.POST, basicUrl, JsonRequestBody(obj))
    headers.forEach { (key, value) ->
      value.forEach { httpRequest.addHeader(key, it) }
    }

    val req = KtorRequestMapper.mapHttpToKtor(httpRequest)

    assertThat(req.method, equalTo(HttpMethod.Post))
    assertThat(req.headers.build(), equalTo(builders.build()))
    assertThat((req.body as TextContent).text, equalTo("{\"string\":\"value\"}"))
  }
}
