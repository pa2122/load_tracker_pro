package com.loadtracker.pro

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ExampleUnitTest {
    private val centralTime = ZoneId.of("America/Chicago")

    @Test
    fun payrollWeekStartsMondayAndEndsSunday() {
        val monday = LocalDate.of(2026, 8, 24)
        val sunday = LocalDate.of(2026, 8, 30)
        val nextMonday = LocalDate.of(2026, 8, 31)

        assertEquals(monday, payrollWeekStart(monday, 0))
        assertEquals(monday, payrollWeekStart(sunday, 23))
        assertEquals(nextMonday, payrollWeekStart(nextMonday, 0))
    }

    @Test
    fun payrollWeekUsesLocalCalendarDateRatherThanUtcDate() {
        val utcMondayBeforeLocalMidnight = Instant.parse("2026-10-05T02:00:00Z").toEpochMilli()
        val utcMondayAtLocalMidnight = Instant.parse("2026-10-05T05:00:00Z").toEpochMilli()

        assertEquals(
            LocalDate.of(2026, 9, 28),
            getPayrollWeekStartDate(utcMondayBeforeLocalMidnight, centralTime)
        )
        assertEquals(
            LocalDate.of(2026, 10, 5),
            getPayrollWeekStartDate(utcMondayAtLocalMidnight, centralTime)
        )
    }

    private fun payrollWeekStart(date: LocalDate, hour: Int): LocalDate {
        val timestamp = date.atTime(hour, 0).atZone(centralTime).toInstant().toEpochMilli()
        return getPayrollWeekStartDate(timestamp, centralTime)
    }
}
