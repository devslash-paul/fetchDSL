package net.devslash.pre

import net.devslash.ListBasedRequestData
import org.junit.Assert.assertTrue
import org.junit.Test

internal class SkipIfTest {

  @Test
  fun testSkipIf() {
    val skip = SkipIf<List<String>> { true }
    val result = skip.skip(ListBasedRequestData(listOf()))
    assertTrue(result)
  }
}

