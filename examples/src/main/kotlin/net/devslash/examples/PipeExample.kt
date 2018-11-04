package net.devslash.examples

import com.sun.net.httpserver.HttpServer
import net.devslash.data.FileDataSupplier
import net.devslash.outputs.FileOut
import net.devslash.pipes.ResettablePipe
import net.devslash.pre.Once
import net.devslash.runHttp
import net.devslash.toPreHook
import java.io.PrintWriter

fun main() {
  val server = HttpServer.create().apply {
    createContext("/") { http ->
      http.responseHeaders.add("Content-type", "text/plain")
      http.sendResponseHeaders(200, 0)
      PrintWriter(http.responseBody).use { out ->
        out.println("Hello ${http.remoteAddress.hostName}!")
      }
    }
  }
  server.start()

  val pipe = ResettablePipe({ r, d -> listOf(String(r.body)) })
  runHttp {
    call(server.address.toString()) {
      data = FileDataSupplier("in.log")
      output {
        +pipe
        +FileOut("!1!")
      }
    }
    call(server.address.toString()) {
      data = pipe
    }
    call(server.address.toString()) {
      preHook {
        +Once({ pipe.reset() }.toPreHook())
      }
      data = pipe
    }
  }
}
