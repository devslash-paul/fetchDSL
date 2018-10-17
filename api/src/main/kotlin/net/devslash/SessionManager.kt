package net.devslash

import kotlinx.coroutines.Job

interface SessionManager {
  suspend fun call(call: Call, jar: CookieJar): MutableList<Job>
  suspend fun call(call: Call): MutableList<Job>
}
