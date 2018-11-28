package net.devslash.outputs

import net.devslash.*
import java.io.PrintStream

class StdOut(private val output: PrintStream = System.out, private val format: OutputFormat = DefaultOutput()) : BasicOutput {
  override fun accept(req: HttpRequest, resp: HttpResponse, data: RequestData) {
    format.accept(resp, data)?.let {
      output.println(String(it))
    }
  }
}
