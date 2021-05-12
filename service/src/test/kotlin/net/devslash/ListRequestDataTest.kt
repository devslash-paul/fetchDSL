package net.devslash

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

internal class ListRequestDataTest {

  @Test
  fun testEmptyRequestData() {
    val data = ListRequestData(emptyList<List<String>>())
    assertThat(data.mustGet<List<String>>(), equalTo(emptyList()))
  }

  @Test
  fun testPopulatedRequestData() {
    val parts = listOf("a", "b", "c", "d", "e")
    val data = ListRequestData(parts).mustGet<List<String>>()

    assertThat(data, equalTo(parts))
  }
}
