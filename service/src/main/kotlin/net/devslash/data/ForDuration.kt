package net.devslash.data

import net.devslash.*
import java.time.Duration
import java.time.Instant

class ForDuration<T>(private val duration: Duration, private val supplier: () -> T, private val clazz: Class<T>) : CallDecorator<T>, RequestDataSupplier<T>, LifecycleController {

  companion object {
    inline operator fun <reified T> invoke(duration: Duration, noinline supplier: () -> T): ForDuration<T> {
      return ForDuration(duration, supplier, T::class.java)
    }
  }

  private var expireTime: Instant? = null

  override fun accept(call: Call<T>): Call<T> {
    return Call(call.url, call.urlProvider, 1, call.rateOptions, call.headers, call.type, this, call.body, call.onError, call.beforeHooks, call.afterHooks, this)
  }

  override fun getRequestExpiry(): Instant {
    return expireTime!!
  }

  override fun getRequestQueueDepth(): Int = 0

  override suspend fun getDataForRequest(): ListRequestData<T>? {
    if (expireTime == null) {
      expireTime = Instant.now().plus(duration)
    } else if (Instant.now().isAfter(expireTime)) {
      return null
    }
    return ListRequestData(supplier.invoke(), clazz)
  }

}
