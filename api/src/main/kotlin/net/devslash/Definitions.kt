package net.devslash

import kotlinx.coroutines.channels.Channel
import java.io.InputStream
import java.util.*

typealias URLProvider<T> = (String, RequestData<T>) -> String
typealias Form = Map<String, List<String>>

interface CallDecorator {
  fun <T> accept(call: Call<T>): Call<T>
}

sealed class FormTypes
class NumberType(val i: Number) : FormTypes()
class StringType(val i: String) : FormTypes()
class ByteArrayType(val i: ByteArray) : FormTypes()

sealed class Body
object EmptyBody: Body()
class JsonRequestBody(val data: Any): Body()
class FormRequestBody(val form: Map<String, List<String>>): Body()
class MultipartFormRequestBody(val form: List<FormPart>): Body()
class StringRequestBody(val body: String): Body()
class BytesRequestBody(val body: InputStream): Body()

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
    val urlProvider: URLProvider<T>?,
    val concurrency: Int?,
    val headers: Map<String, List<HeaderValue>>,
    val type: HttpMethod,
    val dataSupplier: RequestDataSupplier<T>?,
    val body: HttpBody<T>?,
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
  val id: UUID = UUID.randomUUID()
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
      data: T
  )
}

data class BeforeCtx<T>(
    val sessionManager: SessionManager,
    val cookieJar: CookieJar,
    val req: HttpRequest,
    val data: T
)

data class AfterCtx<T>(
    val req: HttpRequest,
    val resp: HttpResponse,
    val data: T
)

@JvmName("beforeAction")
fun <T> BeforeBuilder<T>.action(block: BeforeCtx<T>.() -> Unit) {
  +object : ResolvedSessionPersistingBeforeHook<T> {
    override suspend fun accept(
        sessionManager: SessionManager,
        cookieJar: CookieJar,
        req: HttpRequest,
        data: T
    ) {
      // Safe cast due to usage
      BeforeCtx(sessionManager, cookieJar, req, data).apply(block)
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


class Envelope<T>(private val message: T, private val maxRetries: Int = 3) {
  private var current = 0
  fun get(): T = message
  fun fail(): Int = current++
  fun shouldProceed(): Boolean = current < maxRetries
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
interface ResolvedFullDataAfterHook<T: Any?> : AfterHook {
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

val formIdentity: ValueMapper<Map<String, List<String>>, *> = { form, _ -> form }
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
fun ((HttpResponse) -> Any).toPostHook(): SimpleAfterHook = object : SimpleAfterHook {
  override fun accept(resp: HttpResponse) {
    this@toPostHook(resp)
  }
}

sealed class HttpResult<out T, out E>
data class Success<out T>(val value: T) : HttpResult<T, Nothing>()
data class Failure<out E : Throwable>(val err: E) : HttpResult<Nothing, E>()
