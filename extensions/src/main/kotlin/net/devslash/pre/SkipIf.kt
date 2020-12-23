package net.devslash.pre

import net.devslash.RequestData
import net.devslash.SkipBeforeHook

class SkipIf<T>(private val predicate: (RequestData<T>) -> Boolean): SkipBeforeHook<T> {
  override fun skip(requestData: RequestData<T>): Boolean {
    return predicate(requestData)
  }
}
