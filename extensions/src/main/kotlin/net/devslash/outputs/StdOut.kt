package net.devslash.outputs

import net.devslash.HttpResponse
import net.devslash.RequestData
import net.devslash.BasicOutput

class StdOut : BasicOutput {
  override fun accept(resp: HttpResponse, data: RequestData) {
    println(String(resp.body))
  }
}
