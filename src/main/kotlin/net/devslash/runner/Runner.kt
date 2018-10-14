package net.devslash.runner

import kotlinx.coroutines.Job
import net.devslash.SessionBuilder

suspend fun runHttp(block: SessionBuilder.() -> Unit): List<Job> {
  val session = SessionBuilder().apply(block).build()
  SessionManager(session).run()
  return mutableListOf()
}
