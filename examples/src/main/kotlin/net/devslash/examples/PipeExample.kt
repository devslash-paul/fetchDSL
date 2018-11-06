package net.devslash.examples

import net.devslash.data.FileDataSupplier
import net.devslash.outputs.FileOut
import net.devslash.pipes.ResettablePipe
import net.devslash.runHttp

fun main() {
  val pipe = ResettablePipe({ r, d -> listOf(String(r.body)) })
  EchoServer().use {
    runHttp {
      call(it.address) {
        data = FileDataSupplier(this.javaClass.getResource("/in.log").path)
        output {
          +pipe
          +FileOut("!1!")
        }
      }
      call("${it.address}/a") {
        data = pipe
      }
      call("${it.address}/b") {
        data = pipe
      }
    }
  }
}
