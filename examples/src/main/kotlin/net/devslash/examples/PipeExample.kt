package net.devslash.examples

import io.ktor.server.engine.*
import net.devslash.*
import net.devslash.data.FileDataSupplier
import net.devslash.data.ListDataSupplier
import net.devslash.outputs.WriteFile
import net.devslash.pipes.ResettablePipe
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.experimental.ExperimentalTypeInference


@DslMarker
private annotation class PA

class Test<T> {
  fun add(a: T) {}
  fun secondFun(a: T) {}
  fun <T> Test<T>.lambda(a: (T) -> Unit) {}
}


@OptIn(ExperimentalTypeInference::class)
@PA
fun <T> builder(@BuilderInference x: Test<T>.() -> Unit): Test<T> {
  return Test<T>().apply(x)
}

fun main() {
  val x: Test<Int> = builder {
    add(1)
    lambda { it + 1 }
  }

  val tmp = Files.createTempDirectory("pref")
  val (server, address) = createTestServer()
  val pipe = ResettablePipe({ r, _ -> listOf(String(r.body)) })
  val sp = FileDataSupplier(object {}.javaClass.getResource("/in.log")!!.path)
  try {
    runHttp {
      call(address) {
        data {
          println("Hello!")
          sp
        }
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
            override suspend fun accept(sessionManager: SessionManager, cookieJar: CookieJar, req: HttpRequest, data: List<Any>) {
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
        data { pipe }
        body {
          formParams(mapOf("Yo" to listOf())) { form, _ -> form }
        }
      }
      call(address) {
        data { ListDataSupplier(listOf(1, 2, 3)) }
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
