package net.devslash

import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Logger

data class PollPredicateCtx<T>(val req: HttpRequest, val resp: HttpResponse, val data: T)
typealias PollPredicate<T> = PollPredicateCtx<T>.() -> Boolean

/**
 * PollUntil allows one to set up a DSL context that ensures that a request
 * will poll until such a time that the predicate returns true. Due to
 * how this request type works, the concurrency is set at 1.
 */
class PollUntil<T>(private val predicate: PollPredicate<T>,
                   private val clazz: Class<T>,
                   private val data: T) :
    CallDecorator<T>,
    ResolvedFullDataAfterHook<T>,
    RequestDataSupplier<T> {

  private val logger = Logger.getLogger(PollUntil::class.java.simpleName)
  private val requestAllowed = AtomicBoolean(true)
  private var finished = false

  companion object {
    inline operator fun <reified T> invoke(noinline predicate: PollPredicate<T>,
                                           data: T): PollUntil<T> {
      return PollUntil(predicate, T::class.java, data)
    }

    operator fun invoke(predicate: PollPredicate<List<String>>): PollUntil<List<String>> {
      return PollUntil(predicate, listOf())
    }
  }

  override fun accept(call: Call<T>): Call<T> {
    if (call.dataSupplier != null) {
      logger.warning("Data supplier set, will be overwritten by `PollUntil`")
    }
    if (call.concurrency != 1) {
      logger.info("Call concurrency was " + call.concurrency + ". Setting to 1")
    }

    return Call(call.url, call.urlProvider,
        1, null, call.headers, call.type, this, call.body, call.onError,
        call.beforeHooks, call.afterHooks + this)
  }

  override suspend fun getDataForRequest(): RequestData<T>? {
    while (!requestAllowed.compareAndSet(true, false)) {
      delay(50)
    }

    if (finished) {
      return null
    }

    return ListRequestData(data, clazz)
  }

  override fun accept(req: HttpRequest, resp: HttpResponse, data: T) {
    finished = PollPredicateCtx(req, resp, data).predicate()
    requestAllowed.set(true)
  }
}
