package net.devslash.pre

import net.devslash.RequestData
import net.devslash.SkipBeforeHook

class SkipIf(private val predicate: (RequestData) -> Boolean): SkipBeforeHook {
  override fun skip(requestData: RequestData): Boolean {
    return predicate(requestData)
  }
}
