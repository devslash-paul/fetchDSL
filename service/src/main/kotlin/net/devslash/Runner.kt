package net.devslash

import kotlinx.coroutines.runBlocking

fun runHttp(block: SessionBuilder.() -> Unit) {
  val session = SessionBuilder().apply(block).build()
  HttpSessionManager(session).run().forEach { callList ->
    callList.forEach { job -> runBlocking { job.join() } }
  }
}
