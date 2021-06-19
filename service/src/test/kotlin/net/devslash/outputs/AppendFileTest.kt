package net.devslash.outputs

import net.devslash.ListRequestData
import net.devslash.util.basicRequest
import net.devslash.util.getResponseWithBody
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

internal class AppendFileTest {

  @Rule
  @JvmField
  val tmpDir = TemporaryFolder()

  @Test
  fun testSimpleAppendTest() {
    val file = File(tmpDir.root, "test.log")
    val appender = AppendFile(file.absolutePath)

    appender.accept(
      basicRequest(),
      getResponseWithBody("abc".toByteArray()),
      ListRequestData(listOf<String>())
    )
    appender.accept(
      basicRequest(),
      getResponseWithBody("def".toByteArray()),
      ListRequestData(listOf<String>())
    )

    assertThat(file.readText(), equalTo("abc\ndef\n"))
  }
}
