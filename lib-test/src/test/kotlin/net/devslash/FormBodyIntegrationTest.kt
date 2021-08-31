package net.devslash

import net.devslash.data.ListDataSupplier
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertThrows
import org.junit.ClassRule
import org.junit.Test
import java.lang.ClassCastException
import java.lang.NumberFormatException

class FormBodyIntegrationTest {

  companion object {
    @ClassRule
    @JvmField
    val testServer = TestServer()
  }

  @Test
  fun testFormIndexedReplacements() {
    var result = ""
    runHttp {
      call("${testServer.address()}/form") {
        type = HttpMethod.POST
        data = ListDataSupplier(listOf(listOf("A", "B")))
        body {
          formParams(mapOf("!1!" to listOf("!2!")))
        }
        after {
          action {
            result = String(resp.body)
          }
        }
      }
    }
    assertThat(result, equalTo("A:[B]"))
  }

  @Test
  fun testFormNonIndexed() {
    var result = ""
    runHttp {
      call("${testServer.address()}/form") {
        type = HttpMethod.POST
        body {
          // TODO: Form Params invalid on a get - or at least needs to be repurposed into URL params
          formParams(mapOf("A" to listOf("B")))
        }
        after {
          action {
            result = String(resp.body)
          }
        }
      }
    }
    assertThat(result, equalTo("A:[B]"))
  }

  @Test
  fun testFormNonIndexedWhileTyped() {
    var result = ""
    runHttp {
      call<Int>("${testServer.address()}/form") {
        type = HttpMethod.POST
        data = ListDataSupplier(listOf(1))
        body {
          // TODO: Form Params invalid on a get - or at least needs to be repurposed into URL params
          formParams(mapOf("A" to listOf("B"))) { _, data ->
            mapOf("B" to listOf("C$data"))
          }
        }
        after {
          action {
            result = String(resp.body)
          }
        }
      }
    }
    assertThat(result, equalTo("B:[C1]"))
  }

  @Test
  fun testFormNonIndexedWhileTypedWithNoMapper() {
    var result = ""
    runHttp {
      call<Int>("${testServer.address()}/form") {
        type = HttpMethod.POST
        data = ListDataSupplier(listOf(1))
        body {
          // TODO: Form Params invalid on a get - or at least needs to be repurposed into URL params
          formParams(mapOf("A" to listOf("B")))
        }
        after {
          action {
            result = String(resp.body)
          }
        }
      }
    }
    assertThat(result, equalTo("A:[B]"))
  }

  @Test
  fun testFormIndexedWhileTypedWithNoMapper() {
    var result = ""
    runHttp {
      call<List<Int>>("${testServer.address()}/form") {
        type = HttpMethod.POST
        data = ListDataSupplier(listOf(listOf(1)))
        body {
          formParams(mapOf("A" to listOf("!1!")))
        }
        after {
          action {
            result = String(resp.body)
          }
        }
      }
    }
    assertThat(result, equalTo("A:[1]"))
  }

  @Test
  fun testFormIndexedWhileTypedWithNoMapperAndNoListTypeFails() {
    assertThrows(ClassCastException::class.java) {
      runHttp {
        call<Int>("${testServer.address()}/form") {
          type = HttpMethod.POST
          data = ListDataSupplier(listOf(1))
          body {
            formParams(mapOf("A" to listOf("!1!")))
          }
        }
      }
    }
  }
}
