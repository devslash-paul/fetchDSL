package net.devslash.examples

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import net.devslash.*
import net.devslash.data.FileDataSupplier
import net.devslash.data.ListDataSupplier
import net.devslash.outputs.StdOut
import net.devslash.outputs.WriteFile
import net.devslash.pipes.ResettablePipe
import java.lang.RuntimeException
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
        call.respondText("response here")
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
      call<Int>("") {
        before {

        }
        after {
          action {
            data
          }
        }
      }
      call(address) {
        data = pipe
        before {

          action {
            val x = data
            println("ActionBefore")
          }
        }
        after {
          +object : ResolvedFullDataAfterHook<List<String>> {
            override fun accept(req: HttpRequest, resp: HttpResponse, data: List<String>) {
            }
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
            println(data)
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
