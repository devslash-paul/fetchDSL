package net.devslash

import io.opentelemetry.context.Context
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.time.Clock
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

typealias Contents<T> = Pair<HttpRequest, RequestData<T>>

class HttpSessionManager(private val engine: Driver) : SessionManager, AutoCloseable {

  private var count: Int = 0
  private val jobThreadPool = System.getProperty("HTTP_THREAD_POOL_SIZE")?.toInt()
      ?: (Runtime.getRuntime().availableProcessors() * 2)
  private val httpThreadPool = Executors.newFixedThreadPool(jobThreadPool)
  private val dispatcher = httpThreadPool.asCoroutineDispatcher()

  private var lastCall = 0L
  private val clock = Clock.systemUTC()

  fun run(session: Session) {
    val jar = DefaultCookieJar()
    for (call in session.calls) {
      val localJar = call.cookieJar ?: jar
      call(call, session, localJar)?.let {
        throw it
      }
    }
  }

  override fun close() {
    engine.close()
    httpThreadPool.shutdownNow()
    httpThreadPool.awaitTermination(100L, TimeUnit.MILLISECONDS)
  }

  override fun <T> call(call: Call<T>, session: Session): Exception? = call(call, session, DefaultCookieJar())

  override fun <T> call(call: Call<T>, session: Session, jar: CookieJar): Exception? = runBlocking(Dispatchers.Default) {
    val channel: Channel<Envelope<Contents<T>>> = Channel(call.lifecycleController?.getRequestQueueDepth()
        ?: (session.concurrency * 2))
    val callRunner = { c: Call<T> -> call(c, session, jar) }
    launch(Dispatchers.IO + Context.current().asContextElement()) {
      RequestProducer().produceHttp(callRunner, call, jar, channel)
    }

    val afterRequest: MutableList<AfterHook> = mutableListOf(jar)
    afterRequest.addAll(call.afterHooks)

    val delay = session.delay
    val hasDelay = delay != null && delay > 0

    if (hasDelay) {
      println(
          "Delay has been set to $delay ms. This means that after a call has been made, " + //
              "there will be a delay of at least $delay ms before the beginning of the next one.\n" + //
              "Due to a delay being set - the number of HTTP threads has been locked to 1. " + //
              "Effectively `session.concurrency = 1`"
      )
    }

    val limiter = AcquiringRateLimiter(call.rateOptions ?: session.rateOptions)
    val jobs = mutableListOf<Job>()

    // Take the call concurrency before defaulting to the session concurrency
    val concurrency = if (hasDelay) 1 else call.concurrency
        ?: session.concurrency
    val storedException = AtomicReference<Exception>(null)

    repeat(concurrency) {
      jobs += launch(dispatcher + Context.current().asContextElement()) {
        launchHttpProcessor(
            call,
            session,
            limiter,
            afterRequest,
            channel,
            storedException
        )
      }
    }

    jobs.joinAll()
    storedException.get()
  }

  private suspend fun <T> launchHttpProcessor(
      call: Call<T>,
      session: Session,
      rateLimiter: AcquiringRateLimiter,
      afterRequest: List<AfterHook>,
      channel: Channel<Envelope<Pair<HttpRequest, RequestData<T>>>>,
      storedException: AtomicReference<Exception>
  ) {
    for (next in channel) {
      if (storedException.get() != null) {
        // Break out in the event we've detected an exception somewhere
        return
      }
      try {
        rateLimiter.acquire()
        if (next.shouldProceed()) {
          val contents = next.get()

          when (val resp = makeRequest(contents.first, session)) {
            is Failure -> handleFailure(call, channel, next, resp)
            is Success -> handleSuccess(resp, afterRequest, contents)
          }
        }
      } catch (e: Exception) {
        storedException.set(e)
        return
      }
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun <T> handleSuccess(
      resp: Success<HttpResponse>,
      afterRequest: List<AfterHook>,
      contents: Pair<HttpRequest, RequestData<T>>,
  ) {
    count++
    val mappedResponse = resp.value
    afterRequest.forEach {
      when (it) {
        is SimpleAfterHook -> it.accept(mappedResponse.copy())
        is BodyMutatingAfterHook -> it.accept(mappedResponse)
        is FullDataAfterHook -> it.accept(contents.first, mappedResponse, contents.second)
        is ResolvedFullDataAfterHook<*> -> (it as ResolvedFullDataAfterHook<T>)
            .accept(contents.first, mappedResponse, contents.second.get())

        else -> throw RuntimeException("Unexpected after hook type $it")
      }
    }
  }

  private suspend fun <T> handleFailure(
      call: Call<T>,
      channel: Channel<Envelope<Pair<HttpRequest, RequestData<T>>>>,
      next: Envelope<Pair<HttpRequest, RequestData<T>>>,
      exception: Failure<java.lang.Exception>
  ) {
    when (val onError = call.onError) {
      is OnErrorWithState -> onError.accept(channel, next, exception.err)
      else -> {
        throw exception.err
      }
    }
  }


  private suspend fun makeRequest(modelRequest: HttpRequest, session: Session): HttpResult<HttpResponse, java.lang.Exception> {
    maybeDelay(session)
    val result = engine.call(modelRequest)
    lastCall = clock.millis()
    return result
  }

  private suspend fun maybeDelay(session: Session) {
    session.delay?.let {
      require(it > 0)
      // Then between every call, we have to have waited at least that many ms
      val diff = clock.millis() - lastCall
      // we have to wait for the diff. Due to the fact that delays institute a single threaded
      // system, this is safe. Negative delay returns instantly
      delay(it - diff)
    }
  }
}


