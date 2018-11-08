package net.devslash.pre

import net.devslash.RequestData
import net.devslash.SkipPreHook
import java.util.function.Predicate

class SkipIf(private val predicate: (RequestData) -> Boolean): SkipPreHook {
  override fun skip(requestData: RequestData): Boolean {
    return predicate(requestData)
  }
}
