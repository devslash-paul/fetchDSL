package net.devslash.outputs

import net.devslash.BasicOutput
import net.devslash.HttpResponse
import net.devslash.OutputFormat
import net.devslash.RequestData
import java.io.PrintStream

class StdOut(private val output: PrintStream = System.out, private val format: OutputFormat = DefaultOutput()) : BasicOutput {
  override fun accept(resp: HttpResponse, data: RequestData) {
    format.accept(resp, data)?.let {
      output.println(String(it))
    }
  }
}
