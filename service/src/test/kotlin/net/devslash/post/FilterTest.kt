package net.devslash.post

import net.devslash.HttpResponse
import net.devslash.SimpleAfterHook
import net.devslash.post.Filter
import net.devslash.util.basicData
import net.devslash.util.basicRequest
import net.devslash.util.basicResponse
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import java.net.URI

internal class FilterTest {

  @Test
  fun testBasicTrueFilterExecutesBody() {
    var hit = false
    Filter({ true }, {
      +object : SimpleAfterHook {
        override fun accept(resp: HttpResponse) {
          hit = true
        }
      }
    }).accept(basicRequest(), basicResponse(), basicData())
    assertThat(hit, equalTo(true))
  }

  @Test
  fun testBasicFilterFalseExcludesBody() {
    var hit = false
    Filter({ false }, {
      +object : SimpleAfterHook {
        override fun accept(resp: HttpResponse) {
          hit = true
        }
      }
    }).accept(basicRequest(), basicResponse(), basicData())
    assertThat(hit, equalTo(false))
  }

  @Test
  fun testFilterPassesBasedOnPredicate() {
    var hit = false
    val filter = Filter({ it.url.toASCIIString().contains("allowed") }) {
      +object : SimpleAfterHook {
        override fun accept(resp: HttpResponse) {
          hit = true
        }
      }
    }

    val resp = basicResponse()
    resp.url = URI("http://allowed")
    filter.accept(basicRequest(), resp, basicData())
    assertThat(hit, equalTo(true))
  }

  @Test
  fun testFilterRejectsBasedOnPredicate() {
    var hit = false
    val filter = Filter({ it.url.toASCIIString().contains("allowed") }) {
      +object : SimpleAfterHook {
        override fun accept(resp: HttpResponse) {
          hit = true
        }
      }
    }

    val resp = basicResponse()
    resp.url = URI("http://Do_not_pass")
    filter.accept(basicRequest(), resp, basicData())
    assertThat(hit, equalTo(false))
  }
}