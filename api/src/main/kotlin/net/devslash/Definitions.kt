package net.devslash

import kotlinx.coroutines.channels.Channel

sealed class Value
data class StrValue(val value: String) : Value()
data class ProvidedValue(val lambda: (RequestData) -> String) : Value()

interface BodyProvider
data class Session(val calls: List<Call>, val concurrency: Int = 100, val delay: Long)

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
  fun getDataForRequest(): RequestData

  fun hasNext(): Boolean

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

fun (() -> Unit).toPreHook() = object : SimpleBeforeHook {
  override fun accept(req: HttpRequest, data: RequestData) {
    this@toPreHook()
  }
}

interface SessionPersistingBeforeHook : BeforeHook {
  suspend fun accept(sessionManager: SessionManager,
                     cookieJar: CookieJar,
                     req: HttpRequest,
                     data: RequestData)
}

interface SkipBeforeHook : BeforeHook {
  fun skip(requestData: RequestData): Boolean
}

interface SimpleBeforeHook : BeforeHook {
  fun accept(req: HttpRequest, data: RequestData)
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

sealed class Result<T, E>
data class Success<T, E>(val value: T) : Result<T, E>()
data class Failure<T, E>(val err: E) : Result<T, E>()
