package net.devslash.data

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import net.devslash.ListBasedRequestData
import net.devslash.util.getResponse
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

internal class ModifiableSupplierTest {
  @Test
  fun testModifiedWhenNoRequestData() = runBlocking {
    val supplier = ModifiableSupplier(ListDataSupplier(listOf("A")))
    // Simulate an initial request
    supplier.getDataForRequest()
    supplier.add(ListBasedRequestData(listOf("B")))

    val data = supplier.getDataForRequest()!!
    assertThat(data.getReplacements()["!1!"], equalTo("B"))
  }

  @Test(timeout = 300)
  fun testEmptyRequestDataReturnsNull() = runBlocking {
    val supplier = ModifiableSupplier(ListDataSupplier(listOf<String>()))
    assertThat(supplier.getDataForRequest(), nullValue())
  }

  @Test
  fun testThatGetDataBlocks() = runBlocking {
    val supplier = ModifiableSupplier(ListDataSupplier(listOf("A")))
    supplier.getDataForRequest()
    try {
      withTimeout(200) { supplier.getDataForRequest() }
    } catch (e: TimeoutCancellationException) {
      supplier.accept(getResponse())
      assertThat(supplier.getDataForRequest(), nullValue())
    }
    Unit
  }

  @Test(timeout = 100)
  fun testAddedOnlyReturnedOnce() = runBlocking {
    val supplier = ModifiableSupplier(ListDataSupplier(listOf<String>()))
    supplier.add(ListBasedRequestData(listOf()))
    supplier.getDataForRequest()
    supplier.accept(getResponse())
    assertThat(supplier.getDataForRequest(), nullValue())
  }
}
