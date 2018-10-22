package net.devslash.post

import net.devslash.*

class FilterBuilder {
  var posts = mutableListOf<PostHook>()

  operator fun SimplePostHook.unaryPlus() {
    posts = (posts + this).toMutableList()
  }
}

class Filter(private val pred: (HttpResponse) -> Boolean,
             private val builder: FilterBuilder.() -> Unit) : FullDataPostHook {

  override fun accept(req: HttpRequest, resp: HttpResponse, data: RequestData) {
    val current = FilterBuilder().apply(builder)
    if (pred(resp)) {
      current.posts.forEach {
        when (it) {
          is SimplePostHook -> it.accept(resp)
          is ChainReceivingResponseHook -> it.accept(resp)
          is FullDataPostHook -> it.accept(req, resp, data)
        }
      }
    }
  }
}
