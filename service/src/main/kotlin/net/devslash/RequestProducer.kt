package net.devslash

import kotlinx.coroutines.channels.Channel

class RequestCreator {
  companion object {
    fun <T> getRequestFor(call: Call<T>, data: RequestData<T>): HttpRequest {
      val getUrl = getUrlProvider(call)
      val body = getBodyProvider(call, data.get())
      val currentUrl = getUrl(call.url, data)
      val type = call.type
      val req = HttpRequest(type, currentUrl, body)
      call.headers.forEach { (key, value) ->
        value.map {
          when (it) {
            is StrHeaderValue -> it.value
            is ProvidedHeaderValue -> it.lambda(data)
          }
        }.forEach { req.addHeader(key, it) }
      }
      return req
    }
  }
}

class RequestProducer {
  suspend fun <T> produceHttp(
    sessionManager: SessionManager,
    call: Call<T>,
    jar: CookieJar,
    channel: Channel<Envelope<Contents<T>>>
  ) {
    val dataSupplier = handleNoSupplier(call.dataSupplier)

    var nextData: RequestData<T>? = dataSupplier.getDataForRequest()
    while (nextData != null) {
      val data: RequestData<T> = nextData
      val req = RequestCreator.getRequestFor(call, data)

      val preRequest = call.beforeHooks + jar
      try {
        val shouldSkip =
          preRequest.filterIsInstance<SkipBeforeHook>().any { it.skip(data) }
        if (!shouldSkip) {
          preRequest.forEach {
            when (it) {
              is SimpleBeforeHook -> it.accept(req, data)
              is SessionPersistingBeforeHook -> it.accept(
                sessionManager,
                jar,
                req,
                data
              )
              is ResolvedSessionPersistingBeforeHook<*> -> (it as ResolvedSessionPersistingBeforeHook<T>)
                  .accept(sessionManager, jar, req, data.get())
            }
          }
          channel.send(Envelope(Pair(req, data)))
        }
      } catch (e: Exception) {
        //TODO: Allow configuration of what happens here.
        println("An exception occurred when preparing requests. Shutting down further requests")
        e.printStackTrace()
        channel.close(e)
        return
      }
      nextData = dataSupplier.getDataForRequest()
    }
    channel.close()
  }
}
