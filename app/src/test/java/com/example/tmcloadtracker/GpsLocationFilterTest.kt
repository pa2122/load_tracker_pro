package com.example.tmcloadtracker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GpsLocationFilterTest {

    @Test
    fun testValidLocation_ReturnsTrueForNormalUpdate() {
        val isValid = GpsLocationFilter.isValidLocation(
            currentLat = 32.7770,
            currentLong = -96.7970,
            currentAccuracy = 10f,
            currentTimeMs = 1005000L,
            lastLat = 32.7767,
            lastLong = -96.7970,
            lastTimeMs = 1000000L
        )
        assertTrue("Normal GPS update should be valid", isValid)
    }

    @Test
    fun testInaccurateLocation_ReturnsFalseWhenAccuracyExceedsThreshold() {
        val isValid = GpsLocationFilter.isValidLocation(
            currentLat = 32.7767,
            currentLong = -96.7970,
            currentAccuracy = 65f, // > 50 meters
            currentTimeMs = 1000000L,
            lastLat = null,
            lastLong = null,
            lastTimeMs = null
        )
        assertFalse("Inaccurate GPS update (>50m) should be rejected", isValid)
    }
}
