package net.devslash.post

import net.devslash.*

class FilterBuilder {
  var afterHooks: MutableList<AfterHook> = mutableListOf()

  operator fun AfterHook.unaryPlus() {
    afterHooks = (afterHooks + this).toMutableList()
  }
}

/**
 * An after hook that can be provided additional hooks to call in the event that the predicate is true.
 */
@Suppress("unused")
class Filter<T> private constructor(
  private val predicate: (HttpResponse) -> Boolean,
  boolean: Boolean,
  builder: FilterBuilder.() -> Unit
) :FullDataAfterHook {

  companion object {
    operator fun invoke(predicate: (HttpResponse) -> Boolean, b: FilterBuilder.() -> Unit): Filter<List<String>> {
      return Filter(predicate, true, b)
    }

    @JvmName("invokeTyped")
    operator fun <T> invoke(predicate: (HttpResponse) -> Boolean, b: FilterBuilder.() -> Unit): Filter<T> {
      return Filter(predicate, true, b)
    }
  }

  private val builtBlock = FilterBuilder().apply(builder)

  override fun accept(req: HttpRequest, resp: HttpResponse, data: RequestData<*>) {
    if (predicate(resp)) {
      builtBlock.afterHooks.forEach {
        when (it) {
          is SimpleAfterHook -> it.accept(resp)
          is BodyMutatingAfterHook -> it.accept(resp)
          is FullDataAfterHook -> it.accept(req, resp, data)
          is ResolvedFullDataAfterHook<*> -> (it as ResolvedFullDataAfterHook<T>).accept(req, resp, data.get() as T)
          is BasicOutput -> it.accept(req, resp, data)
          else -> throw RuntimeException("Unsupported filter hook. Filter hooks must be SimpleAfterHook, ChainReceivingResponseHook")
        }
      }
    }
  }
}
