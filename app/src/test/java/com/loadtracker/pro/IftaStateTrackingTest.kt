package com.loadtracker.pro

import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test

class IftaStateTrackingTest {

    @Test
    fun test1_StateDetection_ResolvesGpsCoordinatesToStateCodes() {
        val txLat = 32.7767
        val txLong = -96.7970
        val stateTx = IftaStateDetector.detectStateCode(txLat, txLong)
        assertEquals("TX", stateTx)

        val laLat = 30.4515
        val laLong = -91.1871
        val stateLa = IftaStateDetector.detectStateCode(laLat, laLong)
        assertEquals("LA", stateLa)

        val arLat = 34.7465
        val arLong = -92.2896
        val stateAr = IftaStateDetector.detectStateCode(arLat, arLong)
        assertEquals("AR", stateAr)
    }

    @Test
    fun test2_StateMileageAggregation_CalculatesMilesPerStateFromBreadcrumbs() {
        val breadcrumbs = listOf(
            TripBreadcrumb(id = 1, proNumber = "52167364", latitude = 32.7767, longitude = -96.7970, timestamp = 1000000L), // Dallas TX
            TripBreadcrumb(id = 2, proNumber = "52167364", latitude = 32.5251, longitude = -94.7403, timestamp = 1010000L), // Longview TX
            TripBreadcrumb(id = 3, proNumber = "52167364", latitude = 32.4606, longitude = -93.7502, timestamp = 1020000L), // Shreveport LA
            TripBreadcrumb(id = 4, proNumber = "52167364", latitude = 30.4515, longitude = -91.1871, timestamp = 1030000L)  // Baton Rouge LA
        )

        val stateMiles = IftaStateDetector.calculateStateMileage(breadcrumbs)

        assertEquals(2, stateMiles.size)
        assertTrue("TX mileage should be recorded", stateMiles.containsKey("TX"))
        assertTrue("LA mileage should be recorded", stateMiles.containsKey("LA"))
        assertTrue("TX miles should be greater than 100 mi", stateMiles["TX"]!! > 100.0)
        assertTrue("LA miles should be greater than 100 mi", stateMiles["LA"]!! > 100.0)
    }

    private fun assertTrue(message: String, condition: Boolean) {
        Assert.assertTrue(message, condition)
    }
}
