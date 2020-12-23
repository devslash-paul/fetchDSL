package net.devslash

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

internal class ListBasedRequestDataTest {

  @Test
  fun testEmptyRequestData() {
    val data = ListBasedRequestData(emptyList<Any>())
    assertThat(data.getReplacements(), equalTo(emptyMap()))
  }

  @Test
  fun testPopulatedRequestData() {
    val parts = listOf("a", "b", "c", "d", "e")
    val data = ListBasedRequestData(parts).getReplacements()

    parts.forEachIndexed { index, it ->
      assertThat(data["!${index + 1}!"], equalTo(it))
    }
  }

  @Test
  fun testReplacementsCanBeRetrievedMultipleTimes() {
    val parts = listOf("a", "b", "c", "d", "e")
    val requestData = ListBasedRequestData(parts)
    val data = requestData.getReplacements()

    parts.forEachIndexed { index, it ->
      assertThat(data["!${index + 1}!"], equalTo(it))
    }
    assertThat(requestData.getReplacements(), equalTo(data))
  }
}
