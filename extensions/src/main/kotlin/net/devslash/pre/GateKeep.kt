package net.devslash.pre

import net.devslash.*
import java.util.concurrent.Semaphore

var semaphore = Semaphore(0)

fun acquireTicket(count: Int): SimplePreHook {
  semaphore.release(count)

  return object : SimplePreHook {
    override fun accept(req: HttpRequest, data: RequestData) {
      semaphore.acquire()
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
