package net.devslash.it

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.*
import net.devslash.ListBasedRequestData
import net.devslash.RequestData
import net.devslash.RequestDataSupplier
import net.devslash.runHttp
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Stopwatch
import java.io.PrintWriter
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class HttpBenchmark {

  lateinit var server: HttpServer;

  val count = AtomicInteger(0)
  val next = AtomicInteger(0)

  @Before
  fun setup() {
    val ctx = Executors.newFixedThreadPool(10).asCoroutineDispatcher()
    server = HttpServer.create().apply {
      createContext("/") { http ->
        GlobalScope.launch(ctx) {
          println(next.getAndIncrement())
          delay(150)
          http.responseHeaders.add("Content-type", "text/plain")
          http.sendResponseHeaders(200, 0)
          PrintWriter(http.responseBody).use { out ->
            out.println("Hello ${http.remoteAddress.hostName}!")
            out.close()
          }
          http.close()
        }
      }
    }
    server.bind(InetSocketAddress(Inet4Address.getLocalHost(), 0), 0)
    server.executor = Executors.newCachedThreadPool()
    server.start()
  }

  @Test
  fun check() {
    val start = Instant.now()
    runHttp {
      concurrency = 100
      call("http:/" + server.address.toString() + "/") {
        data = object : RequestDataSupplier {
          override fun hasNext(): Boolean {
            return count.get() < 2000
          }

          override fun getDataForRequest(): RequestData {
            count.getAndIncrement()

            return ListBasedRequestData(listOf("A", "B"))
          }
        }
      }
    }
    println(Duration.between(start, Instant.now()).toMillis())
  }

}
