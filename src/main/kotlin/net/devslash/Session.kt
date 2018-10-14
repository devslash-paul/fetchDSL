package net.devslash

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import net.devslash.runner.runHttp


suspend fun main() = coroutineScope {
  async {
    val result = runHttp {

    }
    result.joinAll()
  }.join()
}

