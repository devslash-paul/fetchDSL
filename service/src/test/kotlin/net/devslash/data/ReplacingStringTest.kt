package net.devslash.data

import net.devslash.ListRequestData
import net.devslash.ReplacingString
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

internal class ReplacingStringTest {

  @Test
  fun `Test replacing mixed types`() {
    val inString = "!1! test !2!"
    val out = ListRequestData(listOf(1, 1.0)).visit(ReplacingString(inString))
    assertThat(out, equalTo("1 test 1.0"))
  }

  @Test
  fun `Test replacing complex type`() {
    data class T(val i: String)

    val inString = "!1!"
    val out = ListRequestData(listOf(T("Hi"))).visit(ReplacingString(inString))
    assertThat(out, equalTo("T(i=Hi)"))
  }

  @Test
  fun `Test replacing not including an index`() {
    val inString = " s !2! s "
    val out = ListRequestData(listOf(1, 6)).visit(ReplacingString(inString))
    assertThat(out, equalTo(" s 6 s "))
  }

}