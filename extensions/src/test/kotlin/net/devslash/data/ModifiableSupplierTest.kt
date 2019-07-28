package net.devslash.data

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import net.devslash.ListBasedRequestData
import net.devslash.util.getResponse
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit

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

  @Test
  @Timeout(value = 300, unit = TimeUnit.MILLISECONDS)
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

  @Test
  @Timeout(value = 100, unit = TimeUnit.MILLISECONDS)
  fun testAddedOnlyReturnedOnce() = runBlocking {
    val supplier = ModifiableSupplier(ListDataSupplier(listOf<String>()))
    supplier.add(ListBasedRequestData(listOf()))
    supplier.getDataForRequest()
    supplier.accept(getResponse())
    assertThat(supplier.getDataForRequest(), nullValue())
  }
}
