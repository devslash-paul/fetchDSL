package net.devslash

import kotlinx.coroutines.Job

interface SessionManager {
  fun call(call: Call, jar: CookieJar): MutableList<Job>
  fun call(call: Call): MutableList<Job>
}
