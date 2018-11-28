package net.devslash

@DslMarker
annotation class SessionDsl

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

@SessionDsl
open class CallBuilder(private val url: String) {
  private var cookieJar: String? = null
  var data: RequestDataSupplier? = null
  var body: HttpBody? = null
  var type: HttpMethod = HttpMethod.GET
  var headers: Map<String, List<Any>>? = null
  private var skipRequestIfOutputExists: Boolean = false

  // new style
  private var preHooksList = listOf<BeforeHook>()
  private var postHooksList = listOf<AfterHook>()

  fun before(block: UnaryAddBuilder<BeforeHook>.() -> Unit) {
    preHooksList = UnaryAddBuilder<BeforeHook>().apply(block).build()
  }

  fun after(block: UnaryAddBuilder<AfterHook>.() -> Unit) {
    postHooksList = UnaryAddBuilder<AfterHook>().apply(block).build()
  }

  fun body(block: BodyBuilder.() -> Unit) {
    body = BodyBuilder().apply(block).build()
  }

  fun mapHeaders(m: Map<String, List<Any>>?) : Map<String, List<Value>>? {
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
    return Call(url, mapHeaders(headers), cookieJar, type, data, body,
        skipRequestIfOutputExists, preHooksList, postHooksList)
  }
}

@SessionDsl
class BodyBuilder {
  var value: String? = null
  var formParams: Map<String, List<String>>? = null

  fun build(): HttpBody = HttpBody(value, formParams)
}

@SessionDsl
class SessionBuilder {
  private var calls = mutableListOf<Call>()
  var concurrency = 20

  fun call(url: String, block: CallBuilder.() -> Unit = {}) {
    calls.add(CallBuilder(url).apply(block).build())
  }

  fun build(): Session = Session(calls, concurrency)
}

