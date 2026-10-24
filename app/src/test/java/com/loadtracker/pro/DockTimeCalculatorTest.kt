package com.loadtracker.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class DockTimeCalculatorTest {
    private val minute = 60_000L
    private val arrival = 1_700_000_000_000L

    private fun calc(minsAfter: Long, apptTs: Long? = null, apptType: String? = null) =
        DockTimeCalculator.calculate(
            arrivalTs = arrival,
            apptTs = apptTs,
            apptType = apptType,
            apptText = null,
            nowMs = arrival + minsAfter * minute,
            zone = ZoneOffset.UTC
        )

    @Test
    fun freeTimeCountsDownFromArrival() {
        val info = calc(30)
        assertEquals("0h 30m", info.totalDockStr)
        assertEquals(90L, info.freeMinsRemaining)
        assertFalse(info.isDetentionAccruing)
        assertFalse(info.isCallDispatchAlert)
    }

    @Test
    fun callDispatchAlertAtNinetyMinutes() {
        assertTrue(calc(90).isCallDispatchAlert)
        assertFalse(calc(89).isCallDispatchAlert)
    }

    @Test
    fun detentionAccruesAfterTwoHours() {
        val info = calc(150)
        assertTrue(info.isDetentionAccruing)
        assertEquals(30L, info.billableMins)
        assertFalse(info.isCallDispatchAlert)
    }

    @Test
    fun earlyArrivalStartsClockAtAppointment() {
        val info = calc(60, apptTs = arrival + 45 * minute, apptType = "A")
        assertEquals(15L, info.clockMins)
        assertEquals("1h 0m", info.totalDockStr)
    }

    @Test
    fun beforeOrFcfsWindowStartsClockAtArrival() {
        assertEquals(60L, calc(60, apptTs = arrival + 45 * minute, apptType = "FCFS").clockMins)
        assertEquals(60L, calc(60, apptTs = arrival + 45 * minute, apptType = "B").clockMins)
        assertTrue(calc(60, apptType = "Before").isBeforeWindow)
    }

    @Test
    fun statementIncludesBillableDetention() {
        val statement = DockTimeCalculator.detentionStatement("123", "Acme", calc(150))
        assertTrue(statement.contains("PRO #123"))
        assertTrue(statement.contains("Facility: Acme"))
        assertTrue(statement.contains("Billable Detention Accrued: 0h 30m"))
        assertTrue(statement.contains("Not Specified"))
    }
}
