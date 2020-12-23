package net.devslash

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.time.Clock
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import kotlin.coroutines.CoroutineContext

private typealias Contents<T> = Pair<HttpRequest, RequestData<T>>

internal class HttpSessionManager(val engine: HttpDriver, private val session: Session) : SessionManager {

  private val semaphore = Semaphore(0)
  private var lastCall = 0L
  private val clock = Clock.systemUTC()
  private lateinit var sessionManager: SessionManager

  fun run() {
    val jar = CookieJar()
    engine.use {
      sessionManager = this
      session.calls.map { call(it, jar) }
    }
  }

  private suspend fun <T> produceHttp(
    call: Call<T>,
    jar: CookieJar, channel: Channel<Envelope<Contents<T>>>
  ) {
    val dataSupplier = handleNoSupplier(call.dataSupplier)

    while (true) {
      // This is _relatively_ safe because the type T is captured in the build of the call. Someone using the DSL
      // can't stuff this up
      @Suppress("UNCHECKED_CAST")
      val data = dataSupplier.getDataForRequest() as RequestData<T>? ?: break
      val req = mapHttpRequest(call, data)
      // Call the prerequesites
      val preRequest = call.beforeHooks.toMutableList()
      preRequest.add(jar)

      val shouldSkip =
        preRequest.filter { it is SkipBeforeHook<*> }.any { (it as SkipBeforeHook<T>).skip(data) }
      if (shouldSkip) continue

      preRequest.forEach {
        when (it) {
          is SimpleBeforeHook -> it.accept(req, data)
          is SessionPersistingBeforeHook -> it.accept(
            sessionManager,
            jar,
            req,
            data
          )
        }
      }
      call.headers?.forEach { entry ->
        entry.value.forEach {
          val s = when (it) {
            is StrValue -> it.value
            is ProvidedValue<*> -> (it as ProvidedValue<T>).lambda(data)
          }
          req.addHeader(entry.key, s)
        }
      }

      if (channel.offer(Envelope(Pair(req, data)))) {
        continue
      } else {
        channel.send(Envelope(Pair(req, data)))
      }
    }
    channel.close()
  }

  override fun <T> call(call: Call<T>) = call(call, CookieJar())


  override fun <T> call(call: Call<T>, jar: CookieJar) = runBlocking {
    // Okay, so in here we're going to do the one to many calls we have to to get this to run.
    val channel: Channel<Envelope<Pair<HttpRequest, RequestData<T>>>> =
      Channel(session.concurrency * 2)
    val produceThreadPool = System.getProperty("PRODUCE_THREAD_POOL_SIZE")?.toInt()
      ?: Runtime.getRuntime().availableProcessors()
    val produceExecutor = Executors.newFixedThreadPool(produceThreadPool)
    val produceDispatcher = produceExecutor.asCoroutineDispatcher()
    launch(produceDispatcher) { produceHttp(call, jar, channel) }

    val afterRequest: MutableList<AfterHook> = mutableListOf(jar)
    afterRequest.addAll(call.afterHooks)

    val delay = session.delay
    val hasDelay = delay != null && delay > 0

    if (hasDelay) {
      println("Delay has been set to $delay ms. This means that after a call has been made, " + //
              "there will be a delay of at least $delay ms before the beginning of the next one.\n" + //
              "Due to a delay being set - the number of HTTP threads has been locked to 1. " + //
              "Effectively `session.concurrency = 1`")
    }
    val limiter = AcquiringRateLimiter(session.rateOptions)

    val threadPool = System.getProperty("HTTP_THREAD_POOL_SIZE")?.toInt()
            ?: Runtime.getRuntime().availableProcessors() * 2
    val httpThreadPool = Executors.newFixedThreadPool(if (hasDelay) 1 else threadPool)
    val dispatcher = httpThreadPool.asCoroutineDispatcher()
    semaphore.release(session.concurrency)

    val jobs = mutableListOf<Job>()
    val concurrency = if (hasDelay) 1 else session.concurrency
    repeat(concurrency) {
      jobs += launchHttpProcessor(
        call,
        limiter,
        afterRequest,
        channel,
        dispatcher
      )
    }
    jobs.joinAll()
    produceExecutor.shutdownNow()
    httpThreadPool.shutdownNow()
    Unit
  }

  private fun <T> CoroutineScope.launchHttpProcessor(
    call: Call<T>,
    rateLimiter: AcquiringRateLimiter,
    afterRequest: List<AfterHook>,
    channel: Channel<Envelope<Pair<HttpRequest, RequestData<T>>>>,
    dispatcher: CoroutineContext
  ) = launch(dispatcher) {
    for (next in channel) {
      // ensure that this is a valid request
      if (next.shouldProceed()) {
        val contents = next.get()
        rateLimiter.acquire()
        when (val request = makeRequest(contents.first)) {
          is Failure -> {
            handleFailure(call, channel, next, request)
          }
          is Success -> {
            val resp = engine.mapResponse(request.value)
            afterRequest.forEach {
              when (it) {
                is SimpleAfterHook -> it.accept(resp.copy())
                is ChainReceivingResponseHook -> it.accept(resp)
                is FullDataAfterHook -> it.accept(contents.first, resp, contents.second)
              }
            }
          }
        }
      }
    }
  }

  private suspend fun <T> handleFailure(
    call: Call<T>,
    channel: Channel<Envelope<Pair<HttpRequest, RequestData<T>>>>,
    next: Envelope<Pair<HttpRequest, RequestData<T>>>,
    request: Failure<java.lang.Exception>
  ) {
    call.onError?.let {
      when (it) {
        is ChannelReceiving<*> -> {
          (it as ChannelReceiving<Contents<T>>).accept(channel, next, request.err)
        }
        else -> {
          throw request.err
        }
      }
    }
  }

  private fun <T> mapHttpRequest(call: Call<T>, data: RequestData<T>): HttpRequest {
    val url = getUrlProvider(call, data)
    val body = getBodyProvider(call, data)
    val currentUrl = url.get()
    val type = call.type

    return HttpRequest(type, currentUrl, body)
  }

  private suspend fun makeRequest(modelRequest: HttpRequest): HttpResult<io.ktor.client.response.HttpResponse, java.lang.Exception> {
    maybeDelay()
    val result = engine.call(modelRequest)
    lastCall = clock.millis()
    return result
  }

  private suspend fun maybeDelay() {
    val delay = session.delay
    if (delay != null && delay > 0) {
      // Then between every call, we have to have waited at least that many ms
      val diff = clock.millis() - lastCall
      if (diff < delay) {
        // we have to wait for the diff. Due to the fact that delays institute a single threaded
        // system, this is safe
        delay(delay - diff)
      }
    }
  }
}


