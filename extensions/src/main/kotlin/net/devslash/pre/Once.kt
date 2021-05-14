package net.devslash.pre

import net.devslash.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType

class Once(private val before: BeforeHook) : SessionPersistingBeforeHook {

  private val flag = AtomicBoolean(false)

  override suspend fun accept(
    sessionManager: SessionManager,
    cookieJar: CookieJar,
    req: HttpRequest,
    data: RequestData
  ) {
    if (flag.compareAndSet(false, true)) {

      val methods: KClass<out BeforeHook> = before::class
      for (method in methods.declaredMemberFunctions) {
        val parameters = method.parameters

        val given = mapOf(
          SessionManager::class.starProjectedType to sessionManager,
          CookieJar::class.starProjectedType to cookieJar,
          HttpRequest::class.starProjectedType to req,
          RequestData::class.starProjectedType to data,
          before::class.starProjectedType to before
        )

        val pList = mutableListOf<Any>()
        parameters.forEach { param ->
          given.keys.forEach {
            if (param.type.isSubtypeOf(it)) {
              given[it]?.let { value ->
                pList.add(value)
              }
            }
          }
        }

        if (parameters.size == pList.size && KVisibility.PUBLIC == method.visibility) {
          if (method.isSuspend) {
            method.callSuspend(*pList.toTypedArray())
          } else {
            method.call(*pList.toTypedArray())
          }
          return
        }
      }

      throw InvalidHookException("Unable to find an appropriate hook method to call on $before")
    }
  }
}
