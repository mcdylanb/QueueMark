package com.bookmarkapp.queuemark.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ReadTimeCalculatorTest {

    @Test
    fun `zero words clamps to one minute`() {
        assertEquals(1, ReadTimeCalculator.estimateMinutes(0))
    }

    @Test
    fun `short article clamps to one minute`() {
        assertEquals(1, ReadTimeCalculator.estimateMinutes(100))
    }

    @Test
    fun `exact multiple of reading speed`() {
        assertEquals(1, ReadTimeCalculator.estimateMinutes(200))
        assertEquals(5, ReadTimeCalculator.estimateMinutes(1000))
    }

    @Test
    fun `rounds to nearest minute`() {
        // 999 / 200 = 4.995 -> 5
        assertEquals(5, ReadTimeCalculator.estimateMinutes(999))
        // 1100 / 200 = 5.5 -> 6 (round half up)
        assertEquals(6, ReadTimeCalculator.estimateMinutes(1100))
        // 1080 / 200 = 5.4 -> 5
        assertEquals(5, ReadTimeCalculator.estimateMinutes(1080))
    }
}
