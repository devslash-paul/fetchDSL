package net.devslash

import kotlinx.coroutines.channels.Channel

sealed class FormTypes
class NumberType(val i: Number) : FormTypes()
class StringType(val i: String) : FormTypes()
class ByteArrayType(val i: ByteArray) : FormTypes()
data class FormPart(val key: String, val value: FormTypes)

sealed class Value
data class StrValue(val value: String) : Value()
data class ProvidedValue(val lambda: (RequestData) -> String) : Value()

interface BodyProvider
data class Session(
  val calls: List<Call<*>>,
  val concurrency: Int = 100,
  val delay: Long?,
  val rateOptions: RateLimitOptions
)

typealias URLProvider = (String, RequestData) -> String

data class Call<T>(
  val url: String,
  val urlProvider: URLProvider?,
  val headers: Map<String, List<Value>>?,
  val cookieJar: String?,
  val type: HttpMethod,
  val dataSupplier: RequestDataSupplier<T>?,
  val body: HttpBody?,
  val onError: OnError?,
  val beforeHooks: List<BeforeHook>,
  val afterHooks: List<AfterHook>
)

interface RequestDataSupplier<T> {
  /**
   * Request data should be a closure that is safe to call on a per-request basis
   */
  suspend fun getDataForRequest(): RequestData?

  fun init() {
    // By default this is empty, but implementors can be assured that on a per-call basis, this
    // will be called
  }
}

interface OutputFormat {
  fun accept(resp: HttpResponse, rep: RequestData): ByteArray?
}

// V is the underlying request data, class is used to find that out
// To is return type
typealias RequestVisitor<T, V> = (V, Class<*>) -> T
typealias MustVisitor<T, V> = (V) -> T

interface RequestData {
  fun <T> visit(visitor: RequestVisitor<T, Any?>): T
}

inline fun <reified T> RequestData.mustGet(): T {
  return this.mustVisit<T, T> { a -> a }
}

inline fun <T, reified V> RequestData.mustVisit(crossinline visitor: MustVisitor<T, V>): T {
  return visit { any, clazz ->
    if (V::class.java.isAssignableFrom(clazz)) {
      return@visit visitor(any as V)
    } else {
      throw RuntimeException("Was unable to require visitor class. Was $clazz not ${V::class.java}")
    }
  }
}

interface BasicOutput : FullDataAfterHook

data class HttpBody(
  val bodyValue: String?,
  val bodyValueMapper: ValueMapper<String>?,
  val formData: Map<String, List<String>>?,
  val formMapper: ValueMapper<Map<String, List<String>>>?,
  val multipartForm: List<FormPart>?,
  val lazyMultipartForm: (RequestData.() -> List<FormPart>)?,
  val jsonObject: Any?,
  val lazyJsonObject: ((RequestData) -> Any)?
)

@Deprecated("Type deprecated")
interface ReplaceableValue<T, V> {
  fun get(data: V): T
}

@Deprecated("Replaceable value should be removed, instead please use the ReplacingString instead")
fun String.asReplaceableValue() = object : ReplaceableValue<String, RequestData> {
  override fun get(data: RequestData): String {
    val replacements = data.mustGet<List<String>>().mapIndexed { ind, t ->
      "!${ind + 1}!" to t
    }.toMap()
    var copy = "" + this@asReplaceableValue
    replacements.forEach { (key, value) -> copy = copy.replace(key, value) }
    return copy
  }
}

interface BeforeHook

interface SimpleBeforeHook : BeforeHook {
  fun accept(req: HttpRequest, data: RequestData)
}

data class BeforeCtx(
  val sessionManager: SessionManager,
  val cookieJar: CookieJar,
  val req: HttpRequest,
  val data: RequestData
)

data class AfterCtx(
  val req: HttpRequest,
  val resp: HttpResponse,
  val data: RequestData
)

@JvmName("beforeAction")
fun UnaryAddBuilder<BeforeHook>.action(block: BeforeCtx.() -> Unit) {
  +object : SessionPersistingBeforeHook {
    override suspend fun accept(
      sessionManager: SessionManager,
      cookieJar: CookieJar,
      req: HttpRequest,
      data: RequestData
    ) {
      // Safe cast due to usage
      BeforeCtx(sessionManager, cookieJar, req, data).apply(block)
    }
  }
}

@JvmName("afterAction")
fun UnaryAddBuilder<AfterHook>.action(block: AfterCtx.() -> Unit) {
  +object : FullDataAfterHook {
    override fun accept(req: HttpRequest, resp: HttpResponse, data: RequestData) {
      AfterCtx(req, resp, data).apply(block)
    }
  }
}

fun (() -> Unit).toPreHook() = object : SimpleBeforeHook {
  override fun accept(req: HttpRequest, data: RequestData) {
    this@toPreHook()
  }
}

interface SessionPersistingBeforeHook : BeforeHook {
  suspend fun accept(
    sessionManager: SessionManager,
    cookieJar: CookieJar,
    req: HttpRequest,
    data: RequestData
  )
}

interface SkipBeforeHook : BeforeHook {
  fun skip(requestData: RequestData): Boolean
}

class Envelope<T>(private val message: T, private val maxRetries: Int = 3) {
  var current = 0
  fun get() = message
  fun fail() = current++
  fun shouldProceed() = current < maxRetries
}

interface OnError
interface ChannelReceiving : OnError {
  suspend fun accept(
    channel: Channel<Envelope<Pair<HttpRequest, RequestData>>>,
    envelope: Envelope<Pair<HttpRequest, RequestData>>,
    e: Exception
  )
}

interface AfterHook
interface SimpleAfterHook : AfterHook {
  fun accept(resp: HttpResponse)
}

fun (() -> Any).toPostHook(): SimpleAfterHook = object : SimpleAfterHook {
  override fun accept(resp: HttpResponse) {
    this@toPostHook()
  }
}

fun ((HttpResponse) -> Any).toPostHook(): SimpleAfterHook = object : SimpleAfterHook {
  override fun accept(resp: HttpResponse) {
    this@toPostHook(resp)
  }
}

interface ChainReceivingResponseHook : AfterHook {
  fun accept(resp: HttpResponse)
}

interface FullDataAfterHook : AfterHook {
  fun accept(req: HttpRequest, resp: HttpResponse, data: RequestData)
}

sealed class HttpResult<out T, out E>
data class Success<out T>(val value: T) : HttpResult<T, Nothing>()
data class Failure<out E : Throwable>(val err: E) : HttpResult<Nothing, E>()
