package net.devslash

interface SessionManager {
  fun <T> call(call: Call<T>, jar: CookieJar): Exception?
  fun <T> call(call: Call<T>): Exception?
}
