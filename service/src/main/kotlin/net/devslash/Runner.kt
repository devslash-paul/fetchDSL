package net.devslash

import kotlinx.coroutines.Job

suspend fun runHttp(block: SessionBuilder.() -> Unit): List<Job> {
  val session = SessionBuilder().apply(block).build()
  HttpSessionManager(session).run()
  return mutableListOf()
}
