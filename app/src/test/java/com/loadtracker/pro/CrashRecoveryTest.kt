package com.loadtracker.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashRecoveryTest {

    @Test
    fun test1_CrashRecoveryState_ValidatesStateDataClass() {
        val state = CrashRecoveryHandler.RecoveredCrashState(
            wasRecovered = true,
            proNumber = "52172719",
            activeSegment = "Loaded",
            timestamp = 1700000000000L
        )

        assertTrue("State should be marked as recovered", state.wasRecovered)
        assertEquals("52172719", state.proNumber)
        assertEquals("Loaded", state.activeSegment)
        assertEquals(1700000000000L, state.timestamp)
    }
}
