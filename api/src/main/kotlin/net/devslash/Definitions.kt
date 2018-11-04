package net.devslash

data class Session(val calls: List<Call>, val concurrency: Int = 100)
data class Call(
  val url: String,
  val headers: List<Pair<String, ReplaceableValue<String, RequestData>>>?,
  val cookieJar: String?,
  val output: List<Output>,
  val type: HttpMethod,
  val dataSupplier: RequestDataSupplier?,
  val body: HttpBody?,
  val skipRequestIfOutputExists: Boolean,
  val preHooks: List<PreHook>,
  val postHooks: List<PostHook>
)

interface RequestDataSupplier {
  /**
   * Request data should be a closure that is safe to call on a per-request basis
   */
  fun getDataForRequest(): RequestData

  fun hasNext(): Boolean
}

interface OutputFormat {
  fun accept(f: HttpResponse, rep: RequestData): ByteArray?
}

interface RequestData {
  fun getReplacements(): Map<String, String>
}

interface Output
interface BasicOutput : Output {
  fun accept(resp: HttpResponse, data: RequestData)
}

data class OutputFile(
  val name: String, val append: Boolean, val split: String, val perLine: Boolean
)

data class HttpBody(val value: String?, val bodyParams: List<Pair<String, String>>?)

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

interface PreHook

fun (() -> Unit).toPreHook() = object : SimplePreHook {
  override fun accept(req: HttpRequest, data: RequestData) {
    this@toPreHook()
  }
}

interface SessionPersistingPreHook : PreHook {
  suspend fun accept(
    sessionManager: SessionManager, cookieJar: CookieJar, req: HttpRequest, data: RequestData
  )
}

interface SkipPreHook : PreHook {
  suspend fun skip(requestData: RequestData): Boolean
}

interface SimplePreHook : PreHook {
  fun accept(req: HttpRequest, data: RequestData)
}

interface PostHook
interface SimplePostHook : PostHook {
  fun accept(resp: HttpResponse)
}

interface ChainReceivingResponseHook : PostHook {
  fun accept(resp: HttpResponse)
}

interface FullDataPostHook : PostHook {
  fun accept(req: HttpRequest, resp: HttpResponse, data: RequestData)
}
