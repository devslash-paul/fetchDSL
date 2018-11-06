package net.devslash.examples

import net.devslash.outputs.DebugOutput
import net.devslash.outputs.StdOut
import net.devslash.runHttp

fun main() {
  EchoServer().use {
    runHttp {
      call(it.address) {
        body {
          formParams = listOf("Hi" to "ho")
        }
        output {
          +StdOut(format = DebugOutput())
        }
      }
    }
  }
}
