package net.devslash.pre

import net.devslash.ListBasedRequestData
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SkipIfTest {

  @Test
  fun testSkipIf() {
    val skip = SkipIf { true }
    val result = skip.skip(ListBasedRequestData(listOf()))
    assertTrue(result)
  }
}

