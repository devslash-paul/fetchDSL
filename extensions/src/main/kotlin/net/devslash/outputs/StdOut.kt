package net.devslash.outputs

import net.devslash.BasicOutput
import net.devslash.HttpResponse
import net.devslash.RequestData
import java.io.PrintStream

class StdOut(val output: PrintStream = System.out) : BasicOutput {
  override fun accept(resp: HttpResponse, data: RequestData) {
    output.println(String(resp.body))
  }
}
