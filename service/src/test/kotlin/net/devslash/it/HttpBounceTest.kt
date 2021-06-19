package net.devslash.it

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import kotlinx.coroutines.runBlocking
import net.devslash.*
import net.devslash.HttpMethod
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import java.net.URI

class HttpBounceTest : ServerTest() {
  override lateinit var appEngine: ApplicationEngine

  @Test
  fun testBasicStep() {
    // TODO - Replace this with something that tests the return mapping
    runWith {
      routing {
        get("/testPath") {
          call.respondText("RESULT")
        }
      }
    }

    var bodyResult = ""

    runBlocking {
      runHttp {
        call("$address/testPath") {
          after {
            +{ resp: HttpResponse -> bodyResult = String(resp.body) }.toPostHook()
          }
        }
      }
    }

    assertEquals("RESULT", bodyResult)
  }
}
