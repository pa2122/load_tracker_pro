package com.example.tmcloadtracker

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class ExampleUnitTest {
    @Test
    fun getPayPeriodDate_fridayThroughThursday_groupsToNextFriday() {
        // Friday-to-Thursday Pay Cycle:
        // Friday 08/21 thru Thursday 08/27 -> Pay week ending Friday 08/28/2026
        // Friday 08/28 thru Thursday 09/03 -> Pay week ending Friday 09/04/2026

        val fridayAugust21 = LocalDate.of(2026, 8, 21)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

        val thursdayAugust27 = LocalDate.of(2026, 8, 27)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

        val fridayAugust28 = LocalDate.of(2026, 8, 28)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

        val mondayAugust31 = LocalDate.of(2026, 8, 31)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

        val thursdaySeptember3 = LocalDate.of(2026, 9, 3)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

        val august28WeekEnding = LocalDate.of(2026, 8, 28)
        val september4WeekEnding = LocalDate.of(2026, 9, 4)

        // Previous week (Fri 8/21 - Thu 8/27)
        assertEquals("Friday 8/21 load belongs to 08/28 week ending", august28WeekEnding, getPayPeriodDate(fridayAugust21))
        assertEquals("Thursday 8/27 load belongs to 08/28 week ending", august28WeekEnding, getPayPeriodDate(thursdayAugust27))

        // Next week starting Friday 8/28 (Fri 8/28 - Thu 9/3)
        assertEquals("Friday 8/28 load belongs to 09/04 week ending", september4WeekEnding, getPayPeriodDate(fridayAugust28))
        assertEquals("Monday 8/31 load belongs to 09/04 week ending", september4WeekEnding, getPayPeriodDate(mondayAugust31))
        assertEquals("Thursday 9/3 load belongs to 09/04 week ending", september4WeekEnding, getPayPeriodDate(thursdaySeptember3))
    }
}