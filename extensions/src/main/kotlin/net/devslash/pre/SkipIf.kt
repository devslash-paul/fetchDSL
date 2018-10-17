package net.devslash.pre

import net.devslash.RequestData
import net.devslash.SkipPreHook
import java.util.function.Predicate

class SkipIf(private val predicate: Predicate<RequestData>): SkipPreHook {
  override suspend fun skip(requestData: RequestData): Boolean {
    return predicate.test(requestData)
  }
}
