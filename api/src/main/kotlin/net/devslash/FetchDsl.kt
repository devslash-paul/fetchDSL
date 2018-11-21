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
  private var output: List<Output> = mutableListOf()
  var data: RequestDataSupplier? = null
  var body: HttpBody? = null
  var type: HttpMethod = HttpMethod.GET
  var headers: Map<String, List<String>>? = null
  private var skipRequestIfOutputExists: Boolean = false

  // new style
  private var preHooksList = mutableListOf<PreHook>()
  private var postHooksList = mutableListOf<PostHook>()

  fun preHook(block: UnaryAddBuilder<PreHook>.() -> Unit) {
    preHooksList = UnaryAddBuilder<PreHook>().apply(block).build()
  }

  fun postHook(block: UnaryAddBuilder<PostHook>.() -> Unit) {
    postHooksList = UnaryAddBuilder<PostHook>().apply(block).build()
  }

  fun output(block: UnaryAddBuilder<Output>.() -> Unit) {
    output = UnaryAddBuilder<Output>().apply(block).build()
  }

  fun body(block: BodyBuilder.() -> Unit) {
    body = BodyBuilder().apply(block).build()
  }

  fun build(): Call {
    return Call(url, headers, cookieJar, output, type, data, body,
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

