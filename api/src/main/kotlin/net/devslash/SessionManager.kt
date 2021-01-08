package net.devslash

interface SessionManager {
  fun call(call: Call, jar: CookieJar): Exception?
  fun call(call: Call): Exception?
}
