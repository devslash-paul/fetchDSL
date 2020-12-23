package net.devslash

import net.devslash.data.Acceptor
import net.devslash.data.DataClassAcceptor

/**
 * This is a one-shot. Not a data supplier. Therefore this simply has to accept a list and provide it in subsequent
 * calls
 */
public class GenericRequestData<T>(private val obj: T) : RequestData<T> {

  override fun get(): T = obj
  private val acceptor: Acceptor = visit(obj)

  private fun visit(obj: T): Acceptor {
    return when (obj) {
      else -> DataClassAcceptor(obj)
    }
  }

  public override fun getReplacements(): Map<String, String> {
    throw NotImplementedError()
  }

  override fun accept(v: String): String {
    return acceptor.accept(v)
  }
}
