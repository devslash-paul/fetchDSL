package net.devslash

import kotlinx.coroutines.runBlocking

suspend fun runHttp(block: SessionBuilder.() -> Unit) {
  return runBlocking {
    val session = SessionBuilder().apply(block).build()
    HttpSessionManager(session, coroutineContext).run().forEach { callList ->
      callList.forEach { job -> job.join() }
    }
  }
}
