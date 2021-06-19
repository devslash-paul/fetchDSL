package net.devslash

import kotlinx.coroutines.runBlocking
import net.devslash.data.FileDataSupplier
import org.junit.Assert.assertEquals
import org.junit.Test

internal class FileBasedDataSupplierTest {

  @Test
  fun testBasicFile() = runBlocking {
    val resource = FileDataSupplier::class.java.getResource("/test.log")!!
    val path = resource.path
    val dataSupplier = FileDataSupplier(path, " ")

    val expected = listOf("a", "b", "c", "d")

    for (character in expected) {
      val requestData = dataSupplier.getDataForRequest()
      // the first word is
      assertEquals(requestData!!.mustGet<List<String>>(), listOf(character))
    }
  }

  @Test
  fun testFileWithMultipleWords() = runBlocking {
    val resource = FileDataSupplier::class.java.getResource("/twowords.log")!!
    val path = resource.path
    val dataSupplier = FileDataSupplier(path, " ")

    val expected = listOf(Pair("a", "b"), Pair("c", "d"))

    for ((first, second) in expected) {
      val requestData = dataSupplier.getDataForRequest()
      // the first word is
      assertEquals(requestData!!.mustGet<List<String>>()[0], first)
      assertEquals(requestData.mustGet<List<String>>()[1], second)
    }
  }

  @Test
  fun testWithTabSeparator() = runBlocking {
    val path = FileDataSupplier::class.java.getResource("/tabspaced.log")!!.path
    val dataSupplier = FileDataSupplier(path, "-")

    val expected = listOf(Pair("a", "b"), Pair("c", "d"))

    for ((first, second) in expected) {
      val requestData = dataSupplier.getDataForRequest()
      // the first word is
      assertEquals(requestData!!.mustGet<List<String>>()[0], first)
      assertEquals(requestData.mustGet<List<String>>()[1], second)
    }
  }

}
