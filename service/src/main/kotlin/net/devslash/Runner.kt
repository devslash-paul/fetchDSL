package net.devslash

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO

fun runHttp(block: SessionBuilder.() -> Unit) {
  return runHttp(CIO.create(), block)
}

internal fun runHttp(engine: HttpClientEngine, block: SessionBuilder.() -> Unit) {
  val session = SessionBuilder().apply(block).build()
  HttpSessionManager(engine, session).run()
}
