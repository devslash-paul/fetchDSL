package net.devslash

import net.devslash.err.RetryOnTransitiveError
import java.time.Duration
import java.util.*

/**
 * Version contains the current version defined in the build.gradle root file.
 *
 * This should be used to identify the User-Agent unless explicitly overwritten as it allows for any clients to be
 * able to easily block the DSL in the event that it is being misused against an endpoint
 */
object Version {
  val version: String

  init {
    val prop = Properties()
    prop.load(FetchDSL::class.java.getResourceAsStream("/version.properties"))
    version = requireNotNull(prop.getProperty("version"))
  }
}

@DslMarker
private annotation class FetchDSL

class BeforeBuilder<T> {
  private var hooks = mutableListOf<BeforeHook>()

  private fun add(before: BeforeHook) {
    hooks += before
  }

  operator fun ResolvedSessionPersistingBeforeHook<in T>.unaryPlus() = add(this)
  operator fun SimpleBeforeHook.unaryPlus() = add(this)
  operator fun SkipBeforeHook.unaryPlus() = add(this)
  operator fun SessionPersistingBeforeHook.unaryPlus() = add(this)

  /**
   * If we can't resolve to the <T> type for the resolved after hook, then it's the incorrect type. If we remove this
   * method, then the error is simply that `+` is not found. Leaving it in with the added deprecation notice leads
   * to a more usable experience as the developer is informed that they got wrong
   */
  @JvmName("unaryPlusAny?")
  @Deprecated(level = DeprecationLevel.ERROR, replaceWith = ReplaceWith("Correct type T for the before hook"),
      message = "Incorrect type T used")
  operator fun ResolvedSessionPersistingBeforeHook<out Any?>.unaryPlus() {
  }

  fun build(): List<BeforeHook> = hooks
}

class AfterBuilder<T> {
  private var hooks = mutableListOf<AfterHook>()

  private fun add(after: AfterHook) {
    hooks += after
  }

  operator fun ResolvedFullDataAfterHook<in T>.unaryPlus() = add(this)
  operator fun SimpleAfterHook.unaryPlus() = add(this)
  operator fun BodyMutatingAfterHook.unaryPlus() = add(this)
  operator fun FullDataAfterHook.unaryPlus() = add(this)

  /**
   * If we can't resolve to the <T> type for the resolved after hook, then it's the incorrect type. If we remove this
   * method, then the error is simply that `+` is not found. Leaving it in with the added deprecation notice leads
   * to a more usable experience as the developer is informed that they got wrong
   */
  @JvmName("unaryPlusAny?")
  @Deprecated(level = DeprecationLevel.ERROR, replaceWith = ReplaceWith("Correct type T for the after hook"),
      message = "Incorrect type T used")
  operator fun ResolvedFullDataAfterHook<out Any?>.unaryPlus() {
  }

  fun build(): List<AfterHook> = hooks
}

data class RateLimitOptions(val enabled: Boolean, val count: Int, val duration: Duration)

enum class HttpMethod {
  GET, POST, DELETE, PUT, HEAD, OPTIONS, PATCH
}

@FetchDSL
@Suppress("MemberVisibilityCanBePrivate")
open class CallBuilder<T>(private val url: String = "") {
  constructor(urlProvider: URLProvider<T>) : this("", urlProvider)
  constructor(url: String, urlProvider: URLProvider<T>) : this(url) {
    this.urlProvider = urlProvider
  }

  // A call local concurrency limit
  val concurrency: Int? = null
  var urlProvider: URLProvider<T>? = null
  var data: RequestDataSupplier<T>? = null
  var body: HttpBody? = null
  var type: HttpMethod = HttpMethod.GET
  var headers: Map<String, List<Any>> = mapOf()
  var onError: OnError? = RetryOnTransitiveError()

  private var preHooksList = mutableListOf<BeforeHook>()
  private var postHooksList = mutableListOf<AfterHook>()

  fun before(block: BeforeBuilder<T>.() -> Unit) {
    preHooksList.addAll(BeforeBuilder<T>().apply(block).build())
  }

  fun after(block: AfterBuilder<T>.() -> Unit) {
    postHooksList.addAll(AfterBuilder<T>().apply(block).build())
  }

  fun body(block: BodyBuilder.() -> Unit) {
    body = BodyBuilder().apply(block).build()
  }

  private fun mapHeaders(map: Map<String, List<Any>>): Map<String, List<HeaderValue>> {
    return map.mapValues { entry ->
      entry.value.map { value ->
        when (value) {
          is HeaderValue -> value
          else -> StrHeaderValue(value.toString())
        }
      }
    }
  }

  fun build(): Call<T> {
    val localHeaders = HashMap(headers)
    localHeaders.putIfAbsent(
        "User-Agent",
        listOf("FetchDSL (Apache-HttpAsyncClient + Kotlin, ${Version.version})")
    )
    return Call(
        url, urlProvider, concurrency, mapHeaders(localHeaders), type, data, body, onError,
        preHooksList, postHooksList
    )
  }
}

@Suppress("MemberVisibilityCanBePrivate")
@FetchDSL
class BodyBuilder {
  private var value: String? = null
  private var valueMapper: ValueMapper<String>? = null

  private var formParts: List<FormPart>? = null
  private var lazyMultipartForm: ((RequestData<*>) -> List<FormPart>)? = null

  private var formParams: Form? = null
  private var formMapper: ValueMapper<Map<String, List<String>>>? = null
  var jsonObject: Any? = null
  var lazyJsonObject: ((RequestData<*>) -> Any)? = null

  // This is actually used. The receiver ensures that only the basic case can utilise a non mapped
  // function
  @Suppress("unused")
  fun formParams(params: Map<String, List<String>>) {
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
  fun multipartForm(
      parts: List<FormPart>
  ) {
    this.formParts = parts
  }

  @Suppress("unused")
  fun multipartForm(
      lazyForm: RequestData<*>.() -> List<FormPart>
  ) {
    this.lazyMultipartForm = lazyForm
  }

  @Suppress("unused")
  fun value(value: String) {
    this.value = value
    this.valueMapper = identityValueMapper
  }

  @Suppress("unused")
  fun value(value: String, mapper: (String, RequestData<*>) -> String) {
    this.value = value
    this.valueMapper = mapper
  }

  fun build(): HttpBody {
    return HttpBody(
        value,
        valueMapper,
        formParams,
        formMapper,
        formParts,
        lazyMultipartForm,
        jsonObject,
        lazyJsonObject
    )
  }
}

@Suppress("MemberVisibilityCanBePrivate")
@FetchDSL
class SessionBuilder {
  private var calls = mutableListOf<Call<*>>()

  var concurrency: Int = 20
  var delay: Long? = null
  var rateOptions: RateLimitOptions = RateLimitOptions(false, 0, Duration.ZERO)

  @Suppress("unused")
  fun rateLimit(count: Int, duration: Duration) {
    require(!(duration.isNegative || duration.isZero)) { "Invalid duration, must be more than zero" }
    require(count > 0) { "Count must be positive" }
    rateOptions = RateLimitOptions(true, count, duration)
  }

  @JvmName("genericCall")
  fun <T> call(url: String, block: CallBuilder<T>.() -> Unit = {}) {
    calls.add(CallBuilder<T>(url).apply(block).build())
  }

  fun call(url: String, block: CallBuilder<List<String>>.() -> Unit = {}) {
    calls.add(CallBuilder<List<String>>(url).apply(block).build())
  }

  @JvmName("genericCallWithUrlProvider")
  fun <T> call(urlProvider: URLProvider<T>, block: CallBuilder<T>.() -> Unit = {}) {
    calls.add(CallBuilder<T>(urlProvider).apply(block).build())
  }

  fun call(urlProvider: URLProvider<List<String>>, block: CallBuilder<List<String>>.() -> Unit = {}) {
    calls.add(CallBuilder(urlProvider).apply(block).build())
  }

  @JvmName("genericCallWithUrlAndUrlProvider")
  fun <T> call(url: String, urlProvider: URLProvider<T>, block: CallBuilder<T>.() -> Unit = {}) {
    calls.add(CallBuilder<T>(url, urlProvider).apply(block).build())
  }

  fun call(url: String, urlProvider: URLProvider<List<String>>, block: CallBuilder<List<String>>.() -> Unit = {}) {
    calls.add(CallBuilder(url, urlProvider).apply(block).build())
  }

  fun build(): Session = Session(calls, concurrency, delay, rateOptions)
}

