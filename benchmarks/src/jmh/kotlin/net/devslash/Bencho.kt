package net.devslash

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit


/*
Result "net.devslash.Bencho.run":
  1.159 ±(99.9%) 0.001 s/op [Average]
  (min, avg, max) = (1.157, 1.159, 1.161), stdev = 0.001
  CI (99.9%): [1.158, 1.159] (assumes normal distribution)

 20-jun-2019
Result "net.devslash.Bencho.run":
  1.121 ±(99.9%) 0.002 s/op [Average]
  (min, avg, max) = (1.118, 1.121, 1.125), stdev = 0.002
  CI (99.9%): [1.120, 1.123] (assumes normal distribution)
 */

@Warmup(iterations = 10, time = 10, timeUnit = TimeUnit.SECONDS)
@CompilerControl(CompilerControl.Mode.DONT_INLINE)
@Measurement(iterations = 10, time = 10, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
open class Bencho {

  private lateinit var echo: EchoServer

  @Setup
  fun setup() {
    echo = EchoServer()
    echo.start()
  }

  @TearDown
  fun tearDown() {
    echo.close()
  }

  @Benchmark
  fun run() {
    runHttp {
      concurrency = 10
      call(echo.address) {
        data = ListDataSupplier(listOf(1..50))
      }
    }
  }
}

fun main() {
  Bencho().run()
}
