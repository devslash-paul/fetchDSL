package net.devslash.data

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import net.devslash.ListRequestData
import net.devslash.mustGet
import net.devslash.util.getBasicResponse
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
    supplier.add(ListRequestData(listOf("B")))

    val data = supplier.getDataForRequest()!!
    assertThat(data.mustGet<List<String>>()[0], equalTo("B"))
  }

  @Test(timeout = 300)
  fun testEmptyRequestDataReturnsNull() = runBlocking {
    val supplier = ModifiableSupplier(ListDataSupplier(listOf()))
    assertThat(supplier.getDataForRequest(), nullValue())
  }

  @Test
  fun testThatGetDataBlocks() = runBlocking {
    val supplier = ModifiableSupplier(ListDataSupplier(listOf("A")))
    supplier.getDataForRequest()
    try {
      withTimeout(200) { supplier.getDataForRequest() }
    } catch (e: TimeoutCancellationException) {
      supplier.accept(getBasicResponse())
      assertThat(supplier.getDataForRequest(), nullValue())
    }
    Unit
  }

  @Test(timeout = 100)
  fun testAddedOnlyReturnedOnce() = runBlocking {
    val supplier = ModifiableSupplier(ListDataSupplier(listOf()))
    supplier.add(ListRequestData(listOf<String>()))
    supplier.getDataForRequest()
    supplier.accept(getBasicResponse())
    assertThat(supplier.getDataForRequest(), nullValue())
  }
}
