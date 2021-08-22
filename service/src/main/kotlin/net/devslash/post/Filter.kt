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
class Filter(
  private val predicate: (HttpResponse) -> Boolean,
  builder: FilterBuilder.() -> Unit
) : FullDataAfterHook {

  private val builtBlock = FilterBuilder().apply(builder)

  override fun accept(req: HttpRequest, resp: HttpResponse, data: RequestData<*>) {
    if (predicate(resp)) {
      builtBlock.afterHooks.forEach {
        when (it) {
          is SimpleAfterHook -> it.accept(resp)
          is BodyMutatingAfterHook -> it.accept(resp)
          is FullDataAfterHook -> it.accept(req, resp, data)
          is ResolvedFullDataAfterHook<*> -> TODO("Resolved data hook not implemented for Filter")
          is BasicOutput -> it.accept(req, resp, data)
          else -> throw RuntimeException("Unsupported filter hook. Filter hooks must be SimpleAfterHook, ChainReceivingResponseHook")
        }
      }
    }
  }
}
