package net.devslash.runner.pre

import net.devslash.SimplePostHook
import net.devslash.SimplePreHook
import net.devslash.runner.HttpRequest
import net.devslash.runner.HttpResponse
import net.devslash.runner.RequestData
import java.util.concurrent.Semaphore

var semaphore = Semaphore(0)

fun acquireTicket(count: Int): SimplePreHook {
  semaphore.release(count)

  return object : SimplePreHook {
    override suspend fun accept(req: HttpRequest, data: RequestData) {
      semaphore.acquire()
      Thread.setDefaultUncaughtExceptionHandler { _, _ ->  semaphore.release() }
    }
  }
}

fun releaseTicket(): SimplePostHook {
  return object : SimplePostHook {
    override fun accept(resp: HttpResponse) {
      semaphore.release()
    }
  }
}
