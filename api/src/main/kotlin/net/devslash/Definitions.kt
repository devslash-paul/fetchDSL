package net.devslash

interface BodyProvider
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

  fun init() {
    // By default this is empty, but implementors can be assured that on a per-call basis, this
    // will be called
  }
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
  fun skip(requestData: RequestData): Boolean
}

interface SimplePreHook : PreHook {
  fun accept(req: HttpRequest, data: RequestData)
}

interface PostHook
interface SimplePostHook : PostHook {
  fun accept(resp: HttpResponse)
}

fun (() -> Any).toPostHook(): PostHook = object : SimplePostHook {
  override fun accept(resp: HttpResponse) {
    this@toPostHook()
  }
}

fun ((HttpResponse) -> Any).toPostHook(): PostHook = object: SimplePostHook {
  override fun accept(resp: HttpResponse) {
    this@toPostHook(resp)
  }
}

interface ChainReceivingResponseHook : PostHook {
  fun accept(resp: HttpResponse)
}

interface FullDataPostHook : PostHook {
  fun accept(req: HttpRequest, resp: HttpResponse, data: RequestData)
}
