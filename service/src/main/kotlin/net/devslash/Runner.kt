package net.devslash

import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
fun runHttp(block: SessionBuilder.() -> Unit) {
  val session = SessionBuilder().apply(block).build()
  HttpSessionManager(session).run()
}
