package net.devslash

import kotlinx.coroutines.channels.Channel
import java.io.InputStream
import java.time.Instant
import java.util.*

typealias URLProvider<T> = (String, RequestData<T>) -> String
typealias Form = Map<String, List<String>>

interface CallDecorator<T> {
  fun accept(call: Call<T>): Call<T>
}

sealed class FormTypes
class NumberType(val i: Number) : FormTypes()
class StringType(val i: String) : FormTypes()
class ByteArrayType(val i: ByteArray) : FormTypes()

sealed class Body
object EmptyBody : Body()
class JsonRequestBody(val data: Any) : Body()
class FormRequestBody(val form: Map<String, List<String>>) : Body()
class MultipartFormRequestBody(val form: List<FormPart>) : Body()
class StringRequestBody(val body: String) : Body()
class BytesRequestBody(val body: InputStream) : Body()

data class FormPart(val key: String, val value: FormTypes)
sealed class HeaderValue
data class StrHeaderValue(val value: String) : HeaderValue()

data class ProvidedHeaderValue(val lambda: (RequestData<*>) -> String) :
  HeaderValue()

/**
 * A session that may be passed to the DSL engine to run. Generally these
 * sessions should be created by [SessionBuilder] through the use of the
 * runHttp block. Explicit instantiation should be reserved for testing only.
 *
 * @property calls the list of [Call]s that will be part of this sessions run
 * list. One call may involve many Http requests. A call will occur one after
 * another. All requests in a call are guaranteed to have been completed before
 * the next call begins
 * @property concurrency the number of requests that can be in flight at the
 * same time. Note that in flight includes the time taken to create the
 * connection, as well as any time tied up in the after block processing.
 * @property delay a set wait time between calls.
 * @property rateOptions A global rate limit that is set for all calls within
 * this session. It may be overwritten by a more specific use of [Call.rateOptions]
 */
data class Session(
  val calls: List<Call<*>>,
  val concurrency: Int = 100,
  @Deprecated("delay should be replaced with a more precise use of " +
      "rateLimitOptions", ReplaceWith("rateOptions"))
  val delay: Long?,
  val rateOptions: RateLimitOptions,
)

/**
 * A build call, generally should not be built directly and should be built by
 * using [CallBuilder]. This often happens automatically by using
 * the runHttp block
 *
 * @property urlProvider If provided, the @property [url] will be passed in as
 * the
 * first argument
 * to the URL provider alongside the Request data. This will allow a client
 * to modify the URL based on requestData without using the `![0-9]+!`
 * syntax.
 *
 * If not provided, the URL will use the built-in replacement provide that
 * looks for !1! like patterns to replace with data.

 */
data class Call<T>(
  val url: String,
  val urlProvider: URLProvider<T>?,
  val concurrency: Int?,
  val rateOptions: RateLimitOptions?,
  val headers: Map<String, List<HeaderValue>>,
  val type: HttpMethod,
  val dataSupplier: RequestDataSupplier<T>?,
  val body: HttpBody<T>?,
  val onError: OnError?,
  val beforeHooks: List<BeforeHook>,
  val afterHooks: List<AfterHook>,
  val lifecycleController: LifecycleController? = null
)

interface LifecycleController {
  fun getRequestExpiry(): Instant?
  fun getRequestQueueDepth(): Int?
}

interface RequestDataSupplier<T> {
  /**
   * Request data should be a closure that is safe to call on a per-request basis
   */
  suspend fun getDataForRequest(): RequestData<T>?

  fun init() {
    // By default, this is empty, but implementors can be assured that on a per-call basis, this
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
  val id: UUID by lazy { UUID.randomUUID() }
  abstract fun <T> visit(visitor: RequestVisitor<T, Any?>): T
  abstract fun get(): T
}

inline fun <reified T> RequestData<*>.mustGet(): T {
  return this.mustVisit<T, T> { a -> a }
}

class DSLVisitorException(reason: String) : RuntimeException(reason)

inline fun <T, reified V> RequestData<*>.mustVisit(crossinline visitor: MustVisitor<T, V>): T {
  return visit { any, clazz ->
    if (V::class.java.isAssignableFrom(clazz)) {
      return@visit visitor(any as V)
    } else {
      throw DSLVisitorException("Was unable to find correct visitor class. Was $clazz not ${V::class.java}")
    }
  }
}

interface BasicOutput : FullDataAfterHook

data class HttpBody<T>(
  val bodyValue: String?,
  val bodyValueMapper: ValueMapper<String, T>?,
  val rawValue: ((RequestData<T>) -> InputStream)?,
  val formData: Map<String, List<String>>?,
  val formMapper: ValueMapper<Map<String, List<String>>, T>?,
  val multipartForm: List<FormPart>?,
  val lazyMultipartForm: (RequestData<T>.() -> List<FormPart>)?,
  val jsonObject: Any?,
  val lazyJsonObject: ((RequestData<T>) -> Any)?
)

@Deprecated("Type deprecated")
interface ReplaceableValue<T, V> {
  fun get(data: V): T
}

@Deprecated("Replaceable value should be removed, instead please use ReplacingString instead")
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

interface SkipBeforeHook : BeforeHook {
  fun skip(requestData: RequestData<*>): Boolean
}

// An Abstraction over the session manager that simply does a call.
typealias CallRunner<T> = (Call<T>) -> Exception?

interface SessionPersistingBeforeHook : BeforeHook {
  suspend fun accept(
    subCallRunner: CallRunner<*>,
    cookieJar: CookieJar,
    req: HttpRequest,
    data: RequestData<*>
  )
}

interface ResolvedSessionPersistingBeforeHook<T> : BeforeHook {
  suspend fun accept(
    subCallRunner: CallRunner<T>,
    cookieJar: CookieJar,
    req: HttpRequest,
    data: T
  )
}

data class BeforeCtx<T>(
  val subCallRunner: CallRunner<T>,
  val cookieJar: CookieJar,
  val req: HttpRequest,
  val data: T,
  val id: UUID,
)

data class AfterCtx<T>(
  val req: HttpRequest, val resp: HttpResponse, val data: T
)

@JvmName("beforeAction")
fun <T> BeforeBuilder<T>.action(block: BeforeCtx<T>.() -> Unit) {
  +object : SessionPersistingBeforeHook {
    override suspend fun accept(
      subCallRunner: CallRunner<*>,
      cookieJar: CookieJar,
      req: HttpRequest,
      data: RequestData<*>
    ) {
      BeforeCtx(subCallRunner, cookieJar, req, data.get() as T, data.id).apply(
        block
      )
    }
  }
}

@JvmName("afterAction")
fun <T> AfterBuilder<T>.action(block: AfterCtx<T>.() -> Unit) {
  +object : ResolvedFullDataAfterHook<T> {
    override fun accept(req: HttpRequest, resp: HttpResponse, data: T) {
      AfterCtx(req, resp, data).apply(block)
    }
  }
}

fun (() -> Unit).toPreHook(): SimpleBeforeHook = object : SimpleBeforeHook {
  override fun accept(req: HttpRequest, data: RequestData<*>) {
    this@toPreHook()
  }
}


class Envelope<T>(
  private val message: T,
  private val maxRetries: Int = 3,
  private val expires: Instant?
) {
  private var current = 0
  fun get(): T = message
  fun fail(): Int = current++
  fun shouldProceed(): Boolean {
    return current < maxRetries && (expires == null || Instant.now()
      .isBefore(expires))
  }
}

interface OnError
interface OnErrorWithState : OnError {
  suspend fun <T> accept(
    channel: Channel<Envelope<Pair<HttpRequest, RequestData<T>>>>,
    envelope: Envelope<Pair<HttpRequest, RequestData<T>>>,
    e: Exception
  )
}

sealed interface AfterHook
interface SimpleAfterHook : AfterHook {
  fun accept(resp: HttpResponse)
}

interface BodyMutatingAfterHook : AfterHook {
  fun accept(resp: HttpResponse)
}

interface FullDataAfterHook : AfterHook {
  fun accept(req: HttpRequest, resp: HttpResponse, data: RequestData<*>)
}

/**
 * Used to reify request data into its real type without requiring visitor use.
 * Can be used incorrectly without compilation warnings - use with care.
 */
interface ResolvedFullDataAfterHook<T : Any?> : AfterHook {
  fun accept(req: HttpRequest, resp: HttpResponse, data: T)
}

fun replaceString(changes: Map<String, String>, str: String): String {
  var x = str
  changes.forEach {
    x = x.replace(it.key, it.value)
  }
  return x
}

val indexValueMapper: ValueMapper<String, *> = { inData, reqData ->
  val indexes = reqData.mustGet<List<String>>().mapIndexed { index, string ->
    "!" + (index + 1) + "!" to string
  }.toMap()
  inData.let { entry ->
    return@let replaceString(indexes, entry)
  }
}

val formIdentity: ValueMapper<Map<String, List<String>>, *> =
  { form, _ -> form }
val formIndexed: ValueMapper<Map<String, List<String>>, *> = { form, reqData ->
  // Early return, as an empty form can otherwise automatically
  // Fail out due to the mustGet default
  if (form.isEmpty()) {
    form
  } else {
    val indexes = reqData.mustGet<List<String>>().mapIndexed { index, string ->
      "!" + (index + 1) + "!" to string
    }.toMap()
    form.map { (formKey, formValue) ->
      val key = replaceString(indexes, formKey)
      val value = formValue.map { replaceString(indexes, it) }
      return@map key to value
    }.toMap()
  }
}

typealias ValueMapper<V, T> = (V, RequestData<T>) -> V

@Suppress("unused")
fun (() -> Any).toPostHook(): SimpleAfterHook = object : SimpleAfterHook {
  override fun accept(resp: HttpResponse) {
    this@toPostHook()
  }
}

@Suppress("unused")
fun ((HttpResponse) -> Any).toPostHook(): SimpleAfterHook =
  object : SimpleAfterHook {
    override fun accept(resp: HttpResponse) {
      this@toPostHook(resp)
    }
  }

sealed class HttpResult<out T, out E>
data class Success<out T>(val value: T) : HttpResult<T, Nothing>()
data class Failure<out E : Throwable>(val err: E) : HttpResult<Nothing, E>()
