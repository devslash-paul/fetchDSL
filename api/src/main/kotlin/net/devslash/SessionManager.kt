package net.devslash

interface SessionManager {
  fun <T> call(call: Call<T>, session: Session, jar: CookieJar): Exception?
  fun <T> call(call: Call<T>, session: Session): Exception?
}
