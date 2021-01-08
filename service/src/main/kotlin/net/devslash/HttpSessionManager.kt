package net.devslash

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.time.Clock
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext

typealias Contents = Pair<HttpRequest, RequestData>

class HttpSessionManager(val engine: HttpDriver, private val session: Session) : SessionManager {

  private val semaphore = Semaphore(0)
  private var lastCall = 0L
  private val clock = Clock.systemUTC()
  private lateinit var sessionManager: SessionManager

  fun run() {
    val jar = CookieJar()
    engine.use {
      sessionManager = this
      for (call in session.calls) {
        val res = call(call, jar)
        if (res != null) {
          throw RuntimeException(res)
        }
      }
    }
  }

  private suspend fun produceHttp(call: Call,
                                  jar: CookieJar, channel: Channel<Envelope<Contents>>) {
    val dataSupplier = handleNoSupplier(call.dataSupplier)

    while (true) {
      val data = dataSupplier.getDataForRequest() ?: break
      val req = mapHttpRequest(call, data)
      // Call the prerequesites
      val preRequest = call.beforeHooks.toMutableList()
      preRequest.add(jar)

      val shouldSkip =
              preRequest.filter { it is SkipBeforeHook }.any { (it as SkipBeforeHook).skip(data) }
      if (shouldSkip) continue

      preRequest.forEach {
        when (it) {
          is SimpleBeforeHook -> it.accept(req, data)
          is SessionPersistingBeforeHook -> it.accept(sessionManager, jar, req, data)
        }
      }
      call.headers?.forEach { entry ->
        entry.value.forEach {
          val s = when (it) {
            is StrValue -> it.value
            is ProvidedValue -> it.lambda(data)
          }
          req.addHeader(entry.key, s)
        }
      }

      if(channel.offer(Envelope(Pair(req, data)))) {
        continue
      } else {
        channel.send(Envelope(Pair(req, data)))
      }
    }
    channel.close()
  }

  override fun call(call: Call) = call(call, CookieJar())

  override fun call(call: Call, jar: CookieJar) = runBlocking(Dispatchers.Default) {
    // Okay, so in here we're going to do the one to many calls we have to to get this to run.
    val channel: Channel<Envelope<Pair<HttpRequest, RequestData>>> =
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
    val storedException = AtomicReference<Exception>(null)
    repeat(concurrency) {
      jobs += launchHttpProcessor(
        call,
        limiter,
        afterRequest,
        channel,
        dispatcher,
        storedException
      )
    }
    jobs.joinAll()
    produceExecutor.shutdownNow()
    httpThreadPool.shutdownNow()
    storedException.get()
  }

  private fun CoroutineScope.launchHttpProcessor(
    call: Call,
    rateLimiter: AcquiringRateLimiter,
    afterRequest: List<AfterHook>,
    channel: Channel<Envelope<Pair<HttpRequest, RequestData>>>,
    dispatcher: CoroutineContext,
    storedException: AtomicReference<Exception>
  ) = launch(dispatcher) {
    for (next in channel) {
      // ensure that this is a valid request
      if (storedException.get() != null) {
        break
      }
      try {
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
      } catch (e: Exception) {
        storedException.set(e)
      }
    }
  }

  private suspend fun handleFailure(call: Call,
                                    channel: Channel<Envelope<Pair<HttpRequest, RequestData>>>,
                                    next: Envelope<Pair<HttpRequest, RequestData>>,
                                    request: Failure<java.lang.Exception>) {
    call.onError?.let {
      when (it) {
        is ChannelReceiving<*> -> {
          (it as ChannelReceiving<Contents>).accept(channel, next, request.err)
        }
        else -> {
          throw request.err
        }
      }
    }
  }

  private fun mapHttpRequest(call: Call, data: RequestData): HttpRequest {
    val url = getUrlProvider(call, data)
    val body = getBodyProvider(call, data)
    val currentUrl = url.get()
    val type = call.type

    return HttpRequest(type, currentUrl, body)
  }

  private suspend fun makeRequest(modelRequest: HttpRequest): HttpResult<io.ktor.client.statement.HttpResponse, java.lang.Exception> {
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


