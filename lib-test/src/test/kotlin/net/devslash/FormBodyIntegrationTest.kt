package net.devslash

import net.devslash.data.ListDataSupplier
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.ClassRule
import org.junit.Test

class FormBodyIntegrationTest {

  companion object {
    @ClassRule
    @JvmField
    val testServer = TestServer()
  }

  @Test
  fun testFormIndexedInKeysAndValue() {
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
  fun testBasicFormNonMapped() {
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
  fun testTypedFormMappedWithData() {
    var result = ""
    runHttp {
      call<Int>("${testServer.address()}/form") {
        type = HttpMethod.POST
        data = ListDataSupplier(listOf(1))
        body {
          // TODO: Form Params invalid on a get - or at least needs to be repurposed into URL params
          formParams {
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
  fun testBasicFormNotMappedWhenTyped() {
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
  fun testBasicFormMapped() {
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
  fun testBasicFormNotIndexMappedWhenNonListType() {
    var form = ""
    runHttp {
      call<Int>("${testServer.address()}/form") {
        type = HttpMethod.POST
        data = ListDataSupplier(listOf(1))
        body {
          formParams(mapOf("A" to listOf("!1!")))
        }
        after {
          action {
            form = String(resp.body)
          }
        }
      }
    }

    assertThat(form, equalTo("A:[!1!]"))
  }
}
