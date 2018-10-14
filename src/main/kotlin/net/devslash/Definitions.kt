package net.devslash

import net.devslash.runner.*
import java.util.function.Consumer

data class Session(val calls: List<Call>, val concurrency: Int = 1)
data class Call(val url: String,
                val headers: List<Pair<String, ReplaceableValue<String, RequestData>>>?,
                val cookieJar: String?,
                val output: Output?,
                val type: HttpMethod,
                val dataSupplier: DataSupplier?,
                val body: HttpBody?,
                val skipRequestIfOutputExists: Boolean,
                val preHooks: List<PreHook>,
                val postHooks: List<PostHook>)

data class Csrf(val url: String,
                val jsonPath: String,
                val headers: List<Pair<String, ReplaceableValue<String, RequestData>>>?,
                val into: String)

data class Output(val file: OutputFile?,
                  val consumer: Consumer<String>?,
                  val append: Boolean,
                  val binary: Boolean)

data class OutputFile(val name: String,
                      val append: Boolean,
                      val split: String,
                      val perLine: Boolean)

data class InputFile(val name: String, val split: String)
data class DataSupplier(val requestFile: InputFile?)
data class HttpBody(val value: String?, val bodyParams: List<Pair<String, String>>?)

interface ReplaceableValue<T, V> {
  fun get(data: V): T
}

fun String.asReplaceableValue() = object : ReplaceableValue<String, RequestData> {
  override fun get(data: RequestData): String {
    return this@asReplaceableValue
  }
}

interface PreHook
interface SessionPersistingPreHook : PreHook {
  suspend fun accept(sessionManager: SessionManager,
                     cookieJar: CookieJar,
                     req: HttpRequest,
                     data: RequestData)
}
interface SkipPreHook : PreHook {
  suspend fun skip(requestData: RequestData): Boolean
}

interface SimplePreHook : PreHook {
  suspend fun accept(req: HttpRequest, data: RequestData)
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
