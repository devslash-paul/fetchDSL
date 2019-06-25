package net.devslash

import io.ktor.client.engine.apache.Apache

fun runHttp(block: SessionBuilder.() -> Unit) {
  return runHttp(HttpDriver(Apache.create()), block)
}

internal fun runHttp(engine: HttpDriver, block: SessionBuilder.() -> Unit) {
  val session = SessionBuilder().apply(block).build()
  HttpSessionManager(engine, session).run()
}
