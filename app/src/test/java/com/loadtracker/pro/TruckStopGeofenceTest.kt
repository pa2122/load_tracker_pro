package com.loadtracker.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TruckStopGeofenceTest {

    @Test
    fun test1_TruckStopGeofence_DetectsLovesLocationWithinRadius() {
        // Location right inside Love's #410 property boundary (28.0371, -97.5083)
        val latInside = 28.0375
        val longInside = -97.5080

        val stop = TruckStopGeofenceEngine.findNearbyTruckStop(latInside, longInside)

        assertNotNull("Truck stop match should not be null when inside radius", stop)
        assertEquals("Love's", stop?.chain)
        assertEquals("TX", stop?.state)
    }

    @Test
    fun test2_TruckStopGeofence_ReturnsNullWhenOutsideTruckStopRadius() {
        // Highway location far away from any truck stop
        val latHighway = 31.0000
        val longHighway = -99.0000

        val stop = TruckStopGeofenceEngine.findNearbyTruckStop(latHighway, longHighway)

        assertNull("Truck stop match should be null when on open highway", stop)
    }
}
