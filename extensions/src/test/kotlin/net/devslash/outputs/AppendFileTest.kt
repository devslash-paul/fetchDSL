package net.devslash.outputs

import net.devslash.ListBasedRequestData
import net.devslash.util.getBasicRequest
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

    appender.accept(getBasicRequest(),
        getResponseWithBody("abc".toByteArray()),
        ListBasedRequestData(listOf<String>()))
    appender.accept(getBasicRequest(),
        getResponseWithBody("def".toByteArray()),
        ListBasedRequestData(listOf<String>()))

    assertThat(file.readText(), equalTo("abc\ndef\n"))
  }
}
