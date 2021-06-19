package net.devslash.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

internal class RepeatTest {
    @Test(expected = IllegalStateException::class)
    fun `Test repeat zero invalid`() {
        Repeat(0)
    }

    @Test
    fun `Test single repeat`() = runBlocking {
        val rep = Repeat(1)
        assertNotNull(rep.getDataForRequest())
        assertNull(rep.getDataForRequest())
    }

    @Test
    fun `Test multiple repeats`() = runBlocking {
        val expected = 100
        val rep = Repeat(expected)

        for(i in 0 until expected) {
            assertNotNull(rep.getDataForRequest())
        }
        assertNull(rep.getDataForRequest())
    }
}