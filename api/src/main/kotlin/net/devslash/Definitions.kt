package net.devslash

import kotlinx.coroutines.channels.Channel

sealed class Value
data class StrValue(val value: String) : Value()
data class ProvidedValue(val lambda: (RequestData) -> String) : Value()

interface BodyProvider
data class Session(val calls: List<Call>, val concurrency: Int = 100, val delay: Long?, val rateOptions: RateLimitOptions)

data class Call(val url: String,
                val headers: Map<String, List<Value>>?,
                val cookieJar: String?,
                val type: HttpMethod,
                val dataSupplier: RequestDataSupplier?,
                val body: HttpBody?,
                val onError: OnError?,
                val beforeHooks: List<BeforeHook>,
                val afterHooks: List<AfterHook>)

interface RequestDataSupplier {
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

interface RequestData {
  fun getReplacements(): Map<String, String>
}

interface BasicOutput : FullDataAfterHook

data class HttpBody(val value: String?,
                    val formData: Map<String, List<String>>?,
                    val jsonObject: Any?,
                    val lazyJsonObject: ((RequestData) -> Any)?)

interface ReplaceableValue<T, V> {
  fun get(data: V): T
}

fun String.asReplaceableValue() = object : ReplaceableValue<String, RequestData> {
  override fun get(data: RequestData): String {
    val replacements = data.getReplacements()
    var copy = "" + this@asReplaceableValue
    replacements.forEach { key, value -> copy = copy.replace(key, value) }
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
      BeforeCtx(sessionManager, cookieJar, req, data).apply(block)
    }
  }
}

/*
Aim: Provide a CTX block that people can hook into...
I wonder if i can find out if something is used
probably not
 */
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
interface ChannelReceiving<V> : OnError {
  suspend fun accept(channel: Channel<Envelope<V>>, envelope: Envelope<V>, e: Exception)
}

interface AfterHook
interface SimpleAfterHook : AfterHook {
  fun accept(resp: HttpResponse)
}

fun (() -> Any).toPostHook(): AfterHook = object : SimpleAfterHook {
  override fun accept(resp: HttpResponse) {
    this@toPostHook()
  }
}

fun ((HttpResponse) -> Any).toPostHook(): AfterHook = object : SimpleAfterHook {
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
