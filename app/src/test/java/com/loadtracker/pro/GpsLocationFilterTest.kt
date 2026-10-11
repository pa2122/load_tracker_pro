package com.loadtracker.pro

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
            currentAccuracy = 65f,
            currentTimeMs = 1000000L,
            lastLat = null,
            lastLong = null,
            lastTimeMs = null
        )
        assertFalse("Inaccurate GPS update (>50m) should be rejected", isValid)
    }

    @Test
    fun testInvalidCoordinateValues_ReturnsFalse() {
        val invalidLat = GpsLocationFilter.isValidLocation(
            currentLat = Double.NaN,
            currentLong = -96.7970,
            currentAccuracy = 10f,
            currentTimeMs = 1000000L,
            lastLat = null,
            lastLong = null,
            lastTimeMs = null
        )

        val invalidLong = GpsLocationFilter.isValidLocation(
            currentLat = 32.7767,
            currentLong = Double.POSITIVE_INFINITY,
            currentAccuracy = 10f,
            currentTimeMs = 1000000L,
            lastLat = null,
            lastLong = null,
            lastTimeMs = null
        )

        val invalidRange = GpsLocationFilter.isValidLocation(
            currentLat = 91.0,
            currentLong = -96.7970,
            currentAccuracy = 10f,
            currentTimeMs = 1000000L,
            lastLat = null,
            lastLong = null,
            lastTimeMs = null
        )

        assertFalse("NaN latitude should be rejected", invalidLat)
        assertFalse("Infinite longitude should be rejected", invalidLong)
        assertFalse("Out-of-range latitude should be rejected", invalidRange)
    }
}
