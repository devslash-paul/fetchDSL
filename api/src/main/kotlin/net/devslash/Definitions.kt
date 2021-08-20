package net.devslash

import kotlinx.coroutines.channels.Channel
import java.util.*

typealias URLProvider = (String, RequestData<*>) -> String

sealed class FormTypes
class NumberType(val i: Number) : FormTypes()
class StringType(val i: String) : FormTypes()
class ByteArrayType(val i: ByteArray) : FormTypes()

data class FormPart(val key: String, val value: FormTypes)
sealed class HeaderValue
data class StrHeaderValue(val value: String) : HeaderValue()

data class ProvidedHeaderValue(val lambda: (RequestData<*>) -> String) : HeaderValue()

data class Session(
  val calls: List<Call<*>>,
  val concurrency: Int = 100,
  val delay: Long?,
  val rateOptions: RateLimitOptions
)

data class Call<T>(
  val url: String,
  val urlProvider: URLProvider?,
  val headers: Map<String, List<HeaderValue>>,
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
  suspend fun getDataForRequest(): RequestData<T>?

  fun init() {
    // By default this is empty, but implementors can be assured that on a per-call basis, this
    // will be called
  }
}

interface OutputFormat {
  fun accept(resp: HttpResponse, data: RequestData<*>): ByteArray?
}

// V is the underlying request data, class is used to find that out
// T is return type
typealias RequestVisitor<T, V> = (V, Class<*>) -> T
typealias MustVisitor<T, V> = (V) -> T

abstract class RequestData<T> {
  val id: UUID = UUID.randomUUID()
  abstract fun <T> visit(visitor: RequestVisitor<T, Any?>): T
}

inline fun <reified T> RequestData<*>.mustGet(): T {
  return this.mustVisit<T, T> { a -> a }
}

inline fun <reified T> RequestData<T>.value(): T {
  return this.mustGet()
}

inline fun <T, reified V> RequestData<*>.mustVisit(crossinline visitor: MustVisitor<T, V>): T {
  return visit { any, clazz ->
    if (V::class.java.isAssignableFrom(clazz)) {
      return@visit visitor(any as V)
    } else {
      throw RuntimeException("Was unable to find correct visitor class. Was $clazz not ${V::class.java}")
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
  val lazyMultipartForm: (RequestData<*>.() -> List<FormPart>)?,
  val jsonObject: Any?,
  val lazyJsonObject: ((RequestData<*>) -> Any)?
)

@Deprecated("Type deprecated")
interface ReplaceableValue<T, V> {
  fun get(data: V): T
}

@Deprecated("Replaceable value should be removed, instead please use the ReplacingString instead")
fun String.asReplaceableValue(): ReplaceableValue<String, RequestData<*>> =
  object : ReplaceableValue<String, RequestData<*>> {
    override fun get(data: RequestData<*>): String {
      val replacements = data.mustGet<List<String>>().mapIndexed { ind, t ->
        "!${ind + 1}!" to t
      }.toMap()
      var copy = "" + this@asReplaceableValue
      replacements.forEach { (key, value) -> copy = copy.replace(key, value) }
      return copy
    }
  }

sealed interface BeforeHook

interface SimpleBeforeHook : BeforeHook {
  fun accept(req: HttpRequest, data: RequestData<*>)
}

data class BeforeCtx<T>(
  val sessionManager: SessionManager,
  val cookieJar: CookieJar,
  val req: HttpRequest,
  val data: RequestData<T>
)

data class AfterCtx<T>(
  val req: HttpRequest,
  val resp: HttpResponse,
  val data: RequestData<T>
)

@JvmName("beforeAction")
fun <T> UnaryAddBuilder<T, BeforeHook>.action(block: BeforeCtx<T>.() -> Unit) {
  +object : ResolvedSessionPersistingBeforeHook<T> {
    override suspend fun accept(
      sessionManager: SessionManager,
      cookieJar: CookieJar,
      req: HttpRequest,
      data: RequestData<T>
    ) {
      // Safe cast due to usage
      BeforeCtx(sessionManager, cookieJar, req, data).apply(block)
    }
  }
}

@JvmName("afterAction")
fun <T> UnaryAddBuilder<T, AfterHook>.action(block: AfterCtx<T>.() -> Unit) {
  +object : ResolvedFullDataAfterHook<T> {
    override fun accept(req: HttpRequest, resp: HttpResponse, data: RequestData<T>) {
      AfterCtx(req, resp, data).apply(block)
    }
  }
}

fun (() -> Unit).toPreHook(): SimpleBeforeHook = object : SimpleBeforeHook {
  override fun accept(req: HttpRequest, data: RequestData<*>) {
    this@toPreHook()
  }
}

interface SessionPersistingBeforeHook : BeforeHook {
  suspend fun accept(
    sessionManager: SessionManager,
    cookieJar: CookieJar,
    req: HttpRequest,
    data: RequestData<*>
  )
}

interface ResolvedSessionPersistingBeforeHook<T> : BeforeHook {
  suspend fun accept(
      sessionManager: SessionManager,
      cookieJar: CookieJar,
      req: HttpRequest,
      data: RequestData<T>
  )
}

interface SkipBeforeHook : BeforeHook {
  fun skip(requestData: RequestData<*>): Boolean
}

class Envelope<T>(private val message: T, private val maxRetries: Int = 3) {
  private var current = 0
  fun get(): T = message
  fun fail(): Int = current++
  fun shouldProceed(): Boolean = current < maxRetries
}

interface OnError
interface OnErrorWithState : OnError {
  suspend fun accept(
    channel: Channel<Envelope<Pair<HttpRequest, RequestData<*>>>>,
    envelope: Envelope<Pair<HttpRequest, RequestData<*>>>,
    e: Exception
  )
}

interface AfterHook
interface SimpleAfterHook : AfterHook {
  fun accept(resp: HttpResponse)
}

@Suppress("unused")
fun (() -> Any).toPostHook(): SimpleAfterHook = object : SimpleAfterHook {
  override fun accept(resp: HttpResponse) {
    this@toPostHook()
  }
}

@Suppress("unused")
fun ((HttpResponse) -> Any).toPostHook(): SimpleAfterHook = object : SimpleAfterHook {
  override fun accept(resp: HttpResponse) {
    this@toPostHook(resp)
  }
}

interface BodyMutatingAfterHook : AfterHook {
  fun accept(resp: HttpResponse)
}

interface FullDataAfterHook : AfterHook {
  fun accept(req: HttpRequest, resp: HttpResponse, data: RequestData<*>)
}

interface ResolvedFullDataAfterHook<T> : AfterHook {
  fun accept(req: HttpRequest, resp: HttpResponse, data: RequestData<T>)
}

sealed class HttpResult<out T, out E>
data class Success<out T>(val value: T) : HttpResult<T, Nothing>()
data class Failure<out E : Throwable>(val err: E) : HttpResult<Nothing, E>()
