package net.devslash

import net.devslash.err.RetryOnTransitiveError
import java.time.Duration

@DslMarker
annotation class FetchDSL

enum class HttpMethod {
  GET, POST, DELETE, PUT, HEAD, OPTIONS, PATCH
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

data class RateLimitOptions(val enabled: Boolean, val count: Int, val duration: Duration)

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

  private fun mapHeaders(m: Map<String, List<Any>>?): Map<String, List<Value>>? {
    return m?.let { map ->
      map.map { entry ->
        entry.key to entry.value.map { value ->
          when (value) {
            is String -> StrValue(value)
            is Value -> value
            else -> throw RuntimeException()
          }
        }
      }.toMap()
    }
  }

  fun build(): Call {
    val localHeaders = headers
    if (localHeaders == null || !localHeaders.contains("User-Agent")) {
      val set = mutableMapOf<String, List<Any>>()
      if (localHeaders != null) {
        set.putAll(localHeaders)
      }
      set["User-Agent"] = listOf("FetchDSL (Apache-HttpAsyncClient + Kotlin, version not set)")
      headers = set
    }
    return Call(
      url, mapHeaders(headers), cookieJar, type, data, body,
      onError, preHooksList, postHooksList
    )
  }
}

fun replaceString(changes: Map<String, String>, str: String): String {
  var x = str
  changes.forEach {
    x = x.replace(it.key, it.value)
  }
  return x
}

typealias ValueMapper<V> = (V, RequestData) -> V

val identityValueMapper: ValueMapper<String> = { v, _ -> v }
val indexValueMapper: ValueMapper<String> = { inData, reqData ->
  val indexes = reqData.mustGet<List<String>>().mapIndexed { index, string ->
    "!" + (index + 1) + "!" to string
  }.toMap()
  inData.let { entry ->
    return@let replaceString(indexes, entry)
  }
}

@FetchDSL
class BodyBuilder {
  private var value: String? = null
  private var valueMapper: ValueMapper<String>? = null
  private var formParams: Form? = null
  private var formMapper: ValueMapper<Map<String, List<String>>>? = null
  var jsonObject: Any? = null
  var lazyJsonObject: ((RequestData) -> Any)? = null

  // This is actually used. The receiver ensures that only the basic case can utilise a non mapped
  // function
  @Suppress("unused")
  fun BodyBuilder.formParams(params: Map<String, List<String>>) {
    formParams = params
    formMapper = formIndexed
  }

  fun formParams(
    params: Map<String, List<String>>,
    mapper: ValueMapper<Map<String, List<String>>> = formIdentity
  ) {
    formParams = params
    formMapper = mapper
  }

  @Suppress("unused")
  fun BodyBuilder.value(value: String) {
    this.value = value
    this.valueMapper = identityValueMapper
  }

  @Suppress("unused")
  fun value(value: String, mapper: (String, RequestData) -> String) {
    this.value = value
    this.valueMapper = mapper
  }

  fun build(): HttpBody {
    return HttpBody(value, valueMapper, formParams, formMapper, jsonObject, lazyJsonObject)
  }

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
  var rateOptions: RateLimitOptions = RateLimitOptions(false, 0, Duration.ZERO)

  fun rateLimit(count: Int, duration: Duration) {
    require(!(duration.isNegative || duration.isZero)) { "Invalid duration, must be more than zero" }
    require(count > 0) { "Count must be positive" }
    rateOptions = RateLimitOptions(true, count, duration)
  }

  @JvmName("genericCall")
  fun <T> call(url: String, block: CallBuilder.() -> Unit = {}) {
    calls.add(CallBuilder(url).apply(block).build())
  }

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

  fun build(): Session = Session(calls, concurrency, delay, rateOptions)
}

