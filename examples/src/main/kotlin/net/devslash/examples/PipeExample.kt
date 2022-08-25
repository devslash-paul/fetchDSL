package net.devslash.examples

import io.ktor.server.engine.*
import net.devslash.*
import net.devslash.data.FileDataSupplier
import net.devslash.data.ListDataSupplier
import net.devslash.outputs.WriteFile
import net.devslash.pipes.ResettablePipe
import java.nio.file.Files
import java.util.concurrent.TimeUnit

fun main() {
  val tmp = Files.createTempDirectory("pref")
  val (server, address) = createTestServer()
  val pipe = ResettablePipe({ r, _ -> listOf(String(r.body)) })
  try {
    runHttp {
      call(address) {
        data = FileDataSupplier(this.javaClass.getResource("/in.log")!!.path)
        after {
          +pipe
          +WriteFile("${tmp.toUri().path}/!1!")
        }
      }
      call({ _, data -> data.get()[0] }) {
        data = ListDataSupplier(listOf(address))
      }
      call(address) {
        data = pipe
        before {
          action {
            println("ActionBefore")
          }
          +object : ResolvedSessionPersistingBeforeHook<List<Any>> {
            override suspend fun accept(subCallRunner: CallRunner<List<Any>>, cookieJar: CookieJar, req: HttpRequest, data: List<Any>) {
              println("More generic type available")
            }
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
