package net.devslash

import net.devslash.err.RetryOnTransitiveError

@DslMarker
annotation class FetchDSL

enum class HttpMethod {
  GET, POST
}

class UnaryAddBuilder<T> {
  private var hooks = mutableListOf<T>()

  operator fun T.unaryPlus() {
    hooks = (hooks + this).toMutableList()
  }

  fun build(): MutableList<T> {
    return hooks
  }
}

@FetchDSL
open class CallBuilder(private val url: String) {
  private var cookieJar: String? = null
  var data: RequestDataSupplier? = null
  var body: HttpBody? = null
  var type: HttpMethod = HttpMethod.GET
  var headers: Map<String, List<Any>>? = null
  var onError: OnError? = RetryOnTransitiveError()

  private var preHooksList = mutableListOf<BeforeHook>()
  private var postHooksList = mutableListOf<AfterHook>()

  fun before(block: UnaryAddBuilder<BeforeHook>.() -> Unit) {
    preHooksList.addAll(UnaryAddBuilder<BeforeHook>().apply(block).build())
  }

  fun after(block: UnaryAddBuilder<AfterHook>.() -> Unit) {
    postHooksList.addAll(UnaryAddBuilder<AfterHook>().apply(block).build())
  }

  fun body(block: BodyBuilder.() -> Unit) {
    body = BodyBuilder().apply(block).build()
  }

  private fun mapHeaders(m: Map<String, List<Any>>?) : Map<String, List<Value>>? {
    return m?.let { map ->
      map.map { entry ->
        entry.key to entry.value.map { value ->
          when (value) {
            is String -> StrValue(value)
            is Value  -> value
            else      -> throw RuntimeException()
          }
        }
      }.toMap()
    }
  }

  fun build(): Call {
    val localHeaders = headers
    if(localHeaders == null || !localHeaders.contains("User-Agent")) {
      val set = mutableMapOf<String, List<Any>>()
      if (localHeaders != null) {
        set.putAll(localHeaders)
      }
      set["User-Agent"] = listOf("FetchDSL (Apache-HttpAsyncClient + Kotlin, version not set)")
      headers = set
    }
    return Call(url, mapHeaders(headers), cookieJar, type, data, body,
         onError, preHooksList, postHooksList)
  }
}

@FetchDSL
class BodyBuilder {
  var value: String? = null
  var formParams: Map<String, List<String>>? = null
  var jsonObject: Any? = null
  var lazyJsonObject: ((RequestData) -> Any)? = null

  fun build(): HttpBody = HttpBody(value, formParams, jsonObject, lazyJsonObject)
}

@FetchDSL
class MultiCallBuilder {
  private var calls = mutableListOf<Call>()

  fun call(url: String, block: CallBuilder.() -> Unit = {}) {
    calls.add(CallBuilder(url).apply(block).build())
  }

  fun calls() = calls
}

@FetchDSL
class SessionBuilder {
  private var calls = mutableListOf<Call>()
  private val chained = mutableListOf<List<Call>>()

  var concurrency = 20
  var delay: Long? = null

  fun call(url: String, block: CallBuilder.() -> Unit = {}) {
    calls.add(CallBuilder(url).apply(block).build())
  }

  // TODO: Re-enable when chaining is stable
//  fun chained(block: MultiCallBuilder.(prev: Previous?) -> Unit = {}) {
//    if (chained.isNotEmpty()) {
//      val line = chained.last().line
//
//    }
//    chained.add(MultiCallBuilder().apply(block).calls())
//  }

  fun build(): Session = Session(calls, concurrency, delay)
}

