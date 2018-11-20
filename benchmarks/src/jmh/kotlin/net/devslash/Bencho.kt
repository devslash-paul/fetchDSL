package net.devslash

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger


/*
Result "net.devslash.Bencho.run":
  1.159 ±(99.9%) 0.001 s/op [Average]
  (min, avg, max) = (1.157, 1.159, 1.161), stdev = 0.001
  CI (99.9%): [1.158, 1.159] (assumes normal distribution)
 */

@Warmup(iterations = 10, time = 10, timeUnit = TimeUnit.SECONDS)
@CompilerControl(CompilerControl.Mode.DONT_INLINE)
@Measurement(iterations = 10, time = 10, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.SECONDS)
open class Bencho {

  @Benchmark
  fun run() {
    val count = AtomicInteger(0)
    EchoServer().use {
      it.start()
      runHttp {
        concurrency = 10
        call(it.address) {
          data = object : RequestDataSupplier {
            override fun hasNext(): Boolean {
              return count.get() < 50
            }

            override fun getDataForRequest(): RequestData {
              count.getAndIncrement()

              return ListBasedRequestData(listOf("A", "B"))
            }
          }
        }
      }
    }
  }
}

fun main() {
  Bencho().run()
}
