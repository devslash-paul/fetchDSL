package net.devslash.decorators

import net.devslash.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.pow

class Progress<T>(private val decimals: Int = 0) : CallDecorator<T>, SimpleAfterHook {
  private val multiplier: Int

  init {
    if (decimals > 2 || decimals < 0) {
      throw IllegalArgumentException("Decimals must be between 0 and 2")
    }
    multiplier = max(1, 10.toDouble().pow(decimals).toInt())
  }

  var size: Int? = null
  var ds: Sized? = null

  var settingLatch = object {}
  var current = AtomicInteger(0)
  var currentPc = AtomicInteger(0)

  override fun ordering(): DecoratorOrder = DecoratorOrder.AFTER_DATA_LOCKED

  override fun accept(call: Call<T>): Call<T> {
    if (call.dataSupplier is Sized) {
      ds = call.dataSupplier as Sized
    } else {
      println("Progress requires a Sized data supplier. One was not found, no progress will be shown")
      return call
    }

    return Call.build(call) {
      afterHooks = call.afterHooks + this@Progress
    }
  }

  override fun accept(resp: HttpResponse) {
    if (size == null) {
      synchronized(settingLatch) {
        if (size == null) {
          size = ds!!.size()
        }
      }
    }

    val size = size ?: return

    val current = current.getAndIncrement()
    val pc = current * 100 * multiplier / size
    val currentPcInt = currentPc.get()
    if (pc > currentPcInt) {
      if (currentPc.compareAndSet(currentPcInt, currentPcInt + 1)) {
        if (decimals > 0) {
          println("${currentPcInt * 1.0 / multiplier}%")
        } else {
          println("${currentPcInt / multiplier}%")
        }
      }
    }
  }
}
