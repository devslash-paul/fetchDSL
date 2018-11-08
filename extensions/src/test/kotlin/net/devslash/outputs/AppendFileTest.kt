package net.devslash.outputs

import net.devslash.ListBasedRequestData
import net.devslash.util.getResponseWithBody
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junitpioneer.jupiter.TempDirectory
import java.io.File
import java.nio.file.Path

internal class AppendFileTest {

  @Test
  @ExtendWith(TempDirectory::class)
  fun testSimpleAppendTest(@TempDirectory.TempDir tmpDir: Path) {
    val file = File(tmpDir.toFile(), "test.log")
    val appender = AppendFile(file.absolutePath)

    appender.accept(getResponseWithBody("abc".toByteArray()), ListBasedRequestData())
    appender.accept(getResponseWithBody("def".toByteArray()), ListBasedRequestData())

    assertThat(file.readText(), equalTo("abc\ndef\n"))
  }
}
