package net.devslash.pre

import net.devslash.*
import java.lang.reflect.Modifier
import java.util.concurrent.atomic.AtomicBoolean

class Once(private val before: BeforeHook) : SessionPersistingBeforeHook {

  private val flag = AtomicBoolean(false)

  override suspend fun accept(sessionManager: SessionManager,
                              cookieJar: CookieJar,
                              req: HttpRequest,
                              data: RequestData) {
    if (flag.compareAndSet(false, true)) {
      val methods = before::class.java.methods
      for (method in methods) {
        val parameters = method.parameters

        val given = mapOf(SessionManager::class.java to sessionManager,
            CookieJar::class.java to cookieJar,
            HttpRequest::class.java to req,
            RequestData::class.java to data)
        val pList = mutableListOf<Any>()
        parameters.forEach { param ->
          given.keys.forEach {
            if (param.type.isAssignableFrom(it)) {
              given[it]?.let { value ->
                pList.add(value)
              }
            }
          }
        }

        if (parameters.size == pList.size && method.declaringClass != Object::class.java && Modifier.isPublic(method.modifiers)) {
          method.invoke(before, *pList.toTypedArray())
          return
        }
      }

      throw InvalidHookException("Unable to find an appropriate hook method to call on $before")
    }
  }
}
