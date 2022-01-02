package net.devslash

import net.devslash.data.ListDataSupplier
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.OptionsBuilder
import java.util.concurrent.TimeUnit

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
    sManagerSupplier = HttpSessionManager(HttpDriver(StaticHttpAdapter()))
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
