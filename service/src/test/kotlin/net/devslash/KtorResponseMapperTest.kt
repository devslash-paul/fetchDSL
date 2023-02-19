package net.devslash

import io.ktor.http.*
import io.ktor.util.date.*
import io.ktor.utils.io.*
import kotlinx.coroutines.runBlocking
import net.devslash.util.basicUrl
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.net.URI

internal class KtorResponseMapperTest {

  @Test
  fun testBasicResponseMap() = runBlocking {
    val response = mock<KtorResponse>()
    whenever(response.headers).thenReturn(Headers.Empty)
    whenever(response.status).thenReturn(HttpStatusCode.fromValue(200))
    whenever(response.content).thenReturn(ByteReadChannel.Empty)
    whenever(response.responseTime).thenReturn(GMTDate.START)
    whenever(response.requestTime).thenReturn(GMTDate.START)
    val mapped = KtorResponseMapper { URI(basicUrl) }.mapResponse(response)

    assertThat(mapped.uri.toASCIIString(), equalTo(basicUrl))
    assertThat(mapped.body, equalTo(ByteArray(0)))
    assertThat(mapped.statusCode, equalTo(200))
    assertThat(mapped.headers, equalTo(mapOf()))
  }

  @Test
  fun testComplexResponseMap() = runBlocking {
    val content = "Test".toByteArray()

    val response = mock<KtorResponse>()
    whenever(response.headers).thenReturn(getHeaders())
    whenever(response.status).thenReturn(HttpStatusCode.fromValue(404))
    whenever(response.content).thenReturn(ByteReadChannel(content))
    whenever(response.responseTime).thenReturn(GMTDate.START)
    whenever(response.requestTime).thenReturn(GMTDate.START)
    val mapped = KtorResponseMapper { URI(basicUrl) }.mapResponse(response)

    assertThat(mapped.uri.toASCIIString(), equalTo(basicUrl))
    assertThat(mapped.body, equalTo(content))
    assertThat(mapped.statusCode, equalTo(404))
    assertThat(mapped.headers, equalTo(mapOf("Test_key" to listOf("Test value"))))
  }

  private fun getHeaders(): Headers {
    val builder = HeadersBuilder()
    builder.append("Test_key", "Test value")
    return builder.build()
  }
}
