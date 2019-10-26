package net.devslash

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import java.net.SocketTimeoutException

@ExperimentalCoroutinesApi
internal class HttpDriverTest : ServerTest() {

  override lateinit var appEngine: ApplicationEngine

  @Test
  fun testConfigureTimeout() {
    appEngine = embeddedServer(Netty, serverPort) {
      routing {
        get("/") {
          delay(1200)
          call.respond(HttpStatusCode.OK, "")
        }
      }
    }
    appEngine.start()

    runBlocking {
      HttpDriver(ConfigBuilder().apply {
        socketTimeout = 1000
      }.build()).use {
        val res = it.call(HttpRequest(HttpMethod.GET, address, EmptyBodyProvider))

        assertThat(res, instanceOf(Failure::class.java))
        assertThat((res as Failure).err, instanceOf(SocketTimeoutException::class.java))
      }
    }
  }
}
