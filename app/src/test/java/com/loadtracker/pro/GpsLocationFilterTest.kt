package com.loadtracker.pro

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GpsLocationFilterTest {

    @Test
    fun testValidFixPasses() {
        val now = System.currentTimeMillis()
        val isValid = GpsLocationFilter.isValidFix(
            accuracy = 10.0f,
            locationTime = now,
            currentTime = now,
            distanceMeters = 20.0f,
            timeDeltaSeconds = 5.0
        )
        assertTrue(isValid)
    }

    @Test
    fun testInaccurateFixIsRejected() {
        val now = System.currentTimeMillis()
        val isValid = GpsLocationFilter.isValidFix(
            accuracy = 75.0f, // > 50m max accuracy
            locationTime = now,
            currentTime = now,
            distanceMeters = 20.0f,
            timeDeltaSeconds = 5.0
        )
        assertFalse(isValid)
    }

    @Test
    fun testStaleFixIsRejected() {
        val now = System.currentTimeMillis()
        val isValid = GpsLocationFilter.isValidFix(
            accuracy = 10.0f,
            locationTime = now - 120_000L, // 2 minutes old (> 60s)
            currentTime = now,
            distanceMeters = 20.0f,
            timeDeltaSeconds = 5.0
        )
        assertFalse(isValid)
    }

    @Test
    fun testUnrealisticSpeedJumpIsRejected() {
        val now = System.currentTimeMillis()
        val isValid = GpsLocationFilter.isValidFix(
            accuracy = 10.0f,
            locationTime = now,
            currentTime = now,
            distanceMeters = 1000.0f, // 1000 meters in 2 seconds -> 500 m/s (> 45 m/s)
            timeDeltaSeconds = 2.0
        )
        assertFalse(isValid)
    }

    @Test
    fun testFixOlderThanSixtySecondsIsRejected() {
        val now = 1_000_000L
        val isValid = GpsLocationFilter.isValidFix(
            accuracy = 10f,
            locationTime = now - 60_001L,
            currentTime = now,
            distanceMeters = null,
            timeDeltaSeconds = null
        )
        assertFalse(isValid)
    }

    @Test
    fun testFutureFixBeyondClockSkewIsRejected() {
        val now = 1_000_000L
        val isValid = GpsLocationFilter.isValidFix(
            accuracy = 10f,
            locationTime = now + 10_001L,
            currentTime = now,
            distanceMeters = null,
            timeDeltaSeconds = null
        )
        assertFalse(isValid)
    }

    @Test
    fun testNonIncreasingTimestampIsRejected() {
        val isValid = GpsLocationFilter.isValidFix(
            accuracy = 10f,
            locationTime = 1_000_000L,
            currentTime = 1_000_000L,
            distanceMeters = 10f,
            timeDeltaSeconds = 0.0
        )
        assertFalse(isValid)
    }
}
