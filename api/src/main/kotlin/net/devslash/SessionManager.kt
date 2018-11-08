package net.devslash

interface SessionManager {
  fun call(call: Call, jar: CookieJar)
  fun call(call: Call)
}
