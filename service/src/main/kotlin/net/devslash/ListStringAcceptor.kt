package net.devslash.data

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

public fun interface Acceptor {
  public fun accept(v: String): String
}

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
public annotation class Capture(val replace: String)

public val NoOpAcceptor: Acceptor = Acceptor { it }

private abstract class TypeReference<T> : Comparable<TypeReference<T>> {
  val type: Type =
    (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0]

  override fun compareTo(other: TypeReference<T>) = 0
}

public fun <T> DataClassAcceptor(v: T): Acceptor {
  val type = v!!::class.java
  val fieldPairs = type.declaredFields.filter { it.getAnnotation(Capture::class.java) != null }
    .map { it to it.getDeclaredAnnotation(Capture::class.java).replace }
  return Acceptor {
    var ret = it
    fieldPairs.forEach { pair ->
      val backer = v!!::class.members.find { it.name == pair.first.name }!!
      ret = it.replace(pair.second, backer.call(v).toString())
    }
    ret
  }
}