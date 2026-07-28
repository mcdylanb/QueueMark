package com.bookmarkapp.queuemark.domain

import java.util.Calendar

enum class ReminderPreset { TONIGHT, TOMORROW_MORNING, THIS_WEEKEND }

// Pure epoch math over an injected "now" so every preset is unit-testable.
object ReminderTimeCalculator {

    fun forPreset(preset: ReminderPreset, nowMillis: Long): Long = when (preset) {
        ReminderPreset.TONIGHT -> tonightAt8(nowMillis)
        ReminderPreset.TOMORROW_MORNING -> tomorrowAt9(nowMillis)
        ReminderPreset.THIS_WEEKEND -> saturdayAt10(nowMillis)
    }

    // Today 20:00, or tomorrow 20:00 when it's already past.
    private fun tonightAt8(nowMillis: Long): Long {
        val cal = calendarAt(nowMillis, hour = 20)
        if (cal.timeInMillis <= nowMillis) cal.add(Calendar.DAY_OF_YEAR, 1)
        return cal.timeInMillis
    }

    private fun tomorrowAt9(nowMillis: Long): Long {
        val cal = calendarAt(nowMillis, hour = 9)
        cal.add(Calendar.DAY_OF_YEAR, 1)
        return cal.timeInMillis
    }

    // Upcoming Saturday 10:00 (today if it's Saturday before 10).
    private fun saturdayAt10(nowMillis: Long): Long {
        val cal = calendarAt(nowMillis, hour = 10)
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY || cal.timeInMillis <= nowMillis) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    private fun calendarAt(nowMillis: Long, hour: Int): Calendar =
        Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
}
