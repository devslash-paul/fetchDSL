package net.devslash.post

import net.devslash.*

class FilterBuilder {
  var posts = mutableListOf<AfterHook>()

  operator fun AfterHook.unaryPlus() {
    posts = (posts + this).toMutableList()
  }
}

class Filter(
  private val pred: (HttpResponse) -> Boolean,
  private val builder: FilterBuilder.() -> Unit
) : FullDataAfterHook {

  override fun accept(req: HttpRequest, resp: HttpResponse, data: RequestData) {
    val current = FilterBuilder().apply(builder)
    if (pred(resp)) {
      current.posts.forEach {
        when (it) {
          is SimpleAfterHook -> it.accept(resp)
          is ChainReceivingResponseHook -> it.accept(resp)
          is FullDataAfterHook -> it.accept(req, resp, data)
          is BasicOutput -> it.accept(req, resp, data)
        }
      }
    }
  }
}
