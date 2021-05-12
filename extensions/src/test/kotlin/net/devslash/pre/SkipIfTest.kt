package net.devslash.pre

import net.devslash.ListRequestData
import org.junit.Assert.assertTrue
import org.junit.Test

internal class SkipIfTest {

  @Test
  fun testSkipIf() {
    val skip = SkipIf { true }
    val result = skip.skip(ListRequestData(listOf<String>()))
    assertTrue(result)
  }
}

