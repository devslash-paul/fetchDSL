package net.devslash

import net.devslash.data.FileDataSupplier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class FileBasedDataSupplierTest {

  @Test
  fun testBasicFile() {
    val path = FileDataSupplier::class.java.getResource("/test.log").path
    val dataSupplier = FileDataSupplier(path, " ")

    val expected = listOf("a", "b", "c", "d")

    for (character in expected) {
      val requestData = dataSupplier.getDataForRequest()
      // the first word is
      assertEquals(requestData.getReplacements()["!1!"], character)
    }
  }

  @Test fun testFileWithMultipleWords() {
    val path = FileDataSupplier::class.java.getResource("/twowords.log").path
    val dataSupplier = FileDataSupplier(path, " ")

    val expected = listOf(Pair("a", "b"), Pair("c", "d"))

    for (character in expected) {
      val requestData = dataSupplier.getDataForRequest()
      // the first word is
      assertEquals(requestData.getReplacements()["!1!"], character.first)
      assertEquals(requestData.getReplacements()["!2!"], character.second)
    }
  }

  @Test fun testWithTabSeparator() {
    val path = FileDataSupplier::class.java.getResource("/tabspaced.log").path
    val dataSupplier = FileDataSupplier(path, "-")

    val expected = listOf(Pair("a", "b"), Pair("c", "d"))

    for (character in expected) {
      val requestData = dataSupplier.getDataForRequest()
      // the first word is
      assertEquals(requestData.getReplacements()["!1!"], character.first)
      assertEquals(requestData.getReplacements()["!2!"], character.second)
    }
  }

}
