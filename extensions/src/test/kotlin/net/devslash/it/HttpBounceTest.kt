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
import net.devslash.pre.SkipIf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HttpBounceTest : ServerTest() {
  override lateinit var appEngine: ApplicationEngine

  @Test
  fun testBasicStep() {
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

  @Test
  fun testWithBodyParams() {
    var sentBody = ""
    runWith {
      routing {
        post("/") {
          sentBody = call.receiveText()
        }
      }
    }

    runBlocking {
      runHttp {
        call(address) {
          type = HttpMethod.POST
          headers = mapOf("A" to listOf("B"))
          body {
            value("TestBody")
          }
        }
      }
    }

    assertEquals("TestBody", sentBody)
  }

  @Test
  fun testCanGetMultipleHeaders() {
    var headers: Headers? = null

    runWith {
      routing {
        get("/") {
          headers = call.request.headers
        }
      }
    }

    runHttp {
      call(address) {
        this.headers =
          mapOf("A" to listOf(ProvidedValue { r -> r.visit(ReplacingString("!1!")) + "." }), "C" to listOf("D"))
        data = SingleUseDataSupplier(listOf("Hi"))
      }
    }

    assertEquals("Hi.", headers!!["A"])
    assertEquals("D", headers!!["C"])
  }

  @Test
  fun testPredicateSkipsRequest() {
    var called = false
    runWith {
      routing {
        get("/") {
          called = true
        }
      }
    }
    runBlocking {
      runHttp {
        call(address) {
          before {
            +SkipIf { true }
          }
        }
      }
    }
    assertFalse(called)
  }
}
