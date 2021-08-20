package net.devslash.examples

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import net.devslash.*
import net.devslash.data.FileDataSupplier
import net.devslash.data.ListDataSupplier
import net.devslash.outputs.WriteFile
import net.devslash.pipes.ResettablePipe
import java.net.ServerSocket
import java.nio.file.Files
import java.util.concurrent.TimeUnit

fun main() {
  val tmp = Files.createTempDirectory("pref")
  val pipe = ResettablePipe({ r, _ -> listOf(String(r.body)) })
  val port = ServerSocket(0).use { it.localPort }
  val server = embeddedServer(Netty, port) {
    routing {
      get("/") {
        call.respondText("")
      }
    }
  }
  server.start()
  val address = "http://localhost:$port"
  try {
    runHttp {
      call(address) {
        data = FileDataSupplier(this.javaClass.getResource("/in.log")!!.path)
        after {
          +pipe
          +WriteFile("${tmp.toUri().path}/!1!")
        }
      }
      call(address) {
        data = pipe
        before {

          action {
            val x = data.value()
            println("ActionBefore")
          }
        }
        after {
          +object : FullDataAfterHook {
            override fun accept(req: HttpRequest, resp: HttpResponse, data: RequestData<*>) {
            }
          }
          action {
            println("ActionAfter")
          }
        }
      }
      call(address) {
        data = pipe
        body {
          formParams(mapOf("Yo" to listOf())) { form, _ -> form }
        }
      }
      call<Int>(address) {
        data = ListDataSupplier(listOf(1, 2, 3))
        before {
          action {
            println(data.mustGet<Int>())
          }
        }
        body {
          formParams(mapOf())
        }
      }
    }
  } finally {
    server.stop(10, 10, TimeUnit.MILLISECONDS)
  }
}
