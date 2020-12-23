package net.devslash

import kotlinx.coroutines.channels.Channel

sealed class Value
data class StrValue(val value: String) : Value()
data class ProvidedValue<T>(val lambda: (RequestData<T>) -> String) : Value()

interface BodyProvider
data class Session(val calls: List<Call<*>>, val concurrency: Int = 100, val delay: Long?, val rateOptions: RateLimitOptions)

data class Call<T>(val url: String,
                val headers: Map<String, List<Value>>?,
                val cookieJar: String?,
                val type: HttpMethod,
                val dataSupplier: RequestDataSupplier<T>?,
                val body: HttpBody<T>?,
                val onError: OnError?,
                val beforeHooks: List<BeforeHook>,
                val afterHooks: List<AfterHook>)

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
  fun <T> accept(resp: HttpResponse, rep: RequestData<T>): ByteArray?
}

interface RequestData<T> {
  @Deprecated("Instead, please migrate to utilising Get")
  fun getReplacements(): Map<String, String>
  fun get(): T
  fun accept(v: String): String
}

interface BasicOutput : FullDataAfterHook

data class HttpBody<T>(val value: String?,
                    val formData: Map<String, List<String>>?,
                    val jsonObject: Any?,
                    val lazyJsonObject: ((RequestData<T>) -> Any)?)

interface ReplaceableValue<T, V> {
  fun get(data: V): T
}

@Deprecated("Please use accept in request data")
fun String.asReplaceableValue() = object : ReplaceableValue<String, RequestData<List<String>>> {
  override fun get(data: RequestData<List<String>>): String {
    val replacements = data.getReplacements()
    var copy = "" + this@asReplaceableValue
    replacements.forEach { (key, value) -> copy = copy.replace(key, value) }
    return copy
  }
}

interface BeforeHook

fun (() -> Unit).toPreHook() = object : SimpleBeforeHook {
  override fun <T> accept(req: HttpRequest, data: RequestData<T>) {
    this@toPreHook()
  }
}

interface SessionPersistingBeforeHook : BeforeHook {
  suspend fun <T> accept(sessionManager: SessionManager,
                     cookieJar: CookieJar,
                     req: HttpRequest,
                     data: RequestData<T>)
}

interface SkipBeforeHook<T> : BeforeHook {
  fun skip(requestData: RequestData<T>): Boolean
}

interface SimpleBeforeHook : BeforeHook {
  fun <T> accept(req: HttpRequest, data: RequestData<T>)
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

interface FullDataAfterHook: AfterHook {
  fun <T> accept(req: HttpRequest, resp: HttpResponse, data: RequestData<T>)
}

sealed class HttpResult<out T, out E>
data class Success<out T>(val value: T) : HttpResult<T, Nothing>()
data class Failure<out E : Throwable>(val err: E) : HttpResult<Nothing, E>()
