package net.devslash.examples

import net.devslash.HttpDriver
import net.devslash.HttpSessionManager
import net.devslash.SessionBuilder
import net.devslash.data.ListDataSupplier


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

fun main() {
  var sManagerSupplier: HttpSessionManager = HttpSessionManager(HttpDriver(MockHttpAdapter()))
  val session = { num: Int ->
    SessionBuilder().apply {
      concurrency = num
      call<Int>("http://any") {
        data = ListDataSupplier((1..100000).toList())
      }
    }.build()
  }

  sManagerSupplier.run(session(100))
  sManagerSupplier.close()
}
