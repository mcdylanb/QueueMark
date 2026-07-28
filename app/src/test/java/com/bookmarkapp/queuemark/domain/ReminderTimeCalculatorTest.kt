package com.bookmarkapp.queuemark.domain

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderTimeCalculatorTest {

    // Wednesday 2026-07-29
    private fun at(hour: Int, minute: Int = 0, dayOffset: Int = 0): Long =
        Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 29, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, dayOffset)
        }.timeInMillis

    @Test
    fun `tonight before 8pm lands today at 8pm`() {
        assertEquals(
            at(hour = 20),
            ReminderTimeCalculator.forPreset(ReminderPreset.TONIGHT, at(hour = 14))
        )
    }

    @Test
    fun `tonight after 8pm rolls to tomorrow 8pm`() {
        assertEquals(
            at(hour = 20, dayOffset = 1),
            ReminderTimeCalculator.forPreset(ReminderPreset.TONIGHT, at(hour = 21))
        )
    }

    @Test
    fun `tomorrow morning is next day 9am`() {
        assertEquals(
            at(hour = 9, dayOffset = 1),
            ReminderTimeCalculator.forPreset(ReminderPreset.TOMORROW_MORNING, at(hour = 14))
        )
        // even late at night it is still "tomorrow"
        assertEquals(
            at(hour = 9, dayOffset = 1),
            ReminderTimeCalculator.forPreset(ReminderPreset.TOMORROW_MORNING, at(hour = 23))
        )
    }

    @Test
    fun `weekend from a wednesday is saturday 10am`() {
        // 2026-07-29 is a Wednesday; Saturday is +3 days
        assertEquals(
            at(hour = 10, dayOffset = 3),
            ReminderTimeCalculator.forPreset(ReminderPreset.THIS_WEEKEND, at(hour = 14))
        )
    }

    @Test
    fun `weekend on saturday morning stays today, after 10am jumps a week`() {
        val saturdayEarly = at(hour = 8, dayOffset = 3)
        assertEquals(
            at(hour = 10, dayOffset = 3),
            ReminderTimeCalculator.forPreset(ReminderPreset.THIS_WEEKEND, saturdayEarly)
        )

        val saturdayLate = at(hour = 11, dayOffset = 3)
        assertEquals(
            at(hour = 10, dayOffset = 10),
            ReminderTimeCalculator.forPreset(ReminderPreset.THIS_WEEKEND, saturdayLate)
        )
    }
}
