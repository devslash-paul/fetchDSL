package net.devslash

import net.devslash.data.ListDataSupplier
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.OptionsBuilder
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

  Benchmark                     Mode  Cnt    Score    Error  Units
Bencho.runFiveConcurrency    thrpt   10  872.168 ▒ 28.743  ops/s
Bencho.runTenConcurrency     thrpt   10  448.297 ▒ 26.756  ops/s
Bencho.runThirtyConcurrency  thrpt   10  445.098 ▒ 21.453  ops/s
Bencho.runTwentyConcurrency  thrpt   10  447.075 ▒ 29.683  ops/s
 */

@CompilerControl(CompilerControl.Mode.DONT_INLINE)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Threads(1)
@Fork(value = 2)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
open class Bencho {
  private lateinit var sManagerSupplier: HttpSessionManager
  val session = { num: Int ->
    SessionBuilder().apply {
      concurrency = num
      call<Int>("http://any") {
        data = ListDataSupplier((1..100).toList())
      }
    }.build()
  }

  @Setup
  fun setup() {
    sManagerSupplier = HttpSessionManager(HttpDriver(MockHttpAdapter()))
  }

  @TearDown
  fun tearDown() {
    sManagerSupplier.close()
  }

  @Benchmark
  fun runOneConcurrency() {
    sManagerSupplier.run(session(1))
  }

  @Benchmark
  fun runFiveConcurrency() {
    sManagerSupplier.run(session(5))
  }

  @Benchmark
  fun runTenConcurrency() {
    sManagerSupplier.run(session(10))
  }

  @Benchmark
  fun runTwentyConcurrency() {
    sManagerSupplier.run(session(20))
  }

  @Benchmark
  fun runThirtyConcurrency() {
    sManagerSupplier.run(session(30))
  }
}

fun main() {
  val opt = OptionsBuilder()
      .include(Bencho::class.java.simpleName)
      .build()

  Runner(opt).run()
}
