package net.devslash.runner.pre

import net.devslash.SkipPreHook
import net.devslash.runner.RequestData
import java.util.function.Predicate

class SkipIf(val predicate: Predicate<RequestData>): SkipPreHook {
  override suspend fun skip(requestData: RequestData): Boolean {
    return predicate.test(requestData)
  }
}
