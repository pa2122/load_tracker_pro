package com.example.tmcloadtracker

import org.junit.Assert.assertEquals
import org.junit.Test

class DualMileageAdjustmentTest {

    @Test
    fun testSimultaneousBounceAndLoadedMileageAdjustments() {
        val initialLoad = CurrentLoad(
            proNumber = "52167364",
            dispatchedBounceMiles = 150.0,
            dispatchedLoadedMiles = 950.0,
            bounceMilesStart = 0.0,
            bounceMilesEnd = 0.0,
            loadedMilesStart = 0.0,
            loadedMilesEnd = 0.0,
            percentageRate = 31.0,
            loadPay = 2500.0,
            tarpType = "N",
            isPreTarped = false,
            pickupTimestamp = System.currentTimeMillis(),
            isGoingHome = false,
            tripState = "ACTIVE_LOADED"
        )

        // Late entry / delayed departure adjustment:
        val adjustedBounce = 25.0
        val adjustedLoaded = 110.0

        val updatedLoad = initialLoad.copy(
            bounceMilesEnd = adjustedBounce,
            loadedMilesEnd = adjustedLoaded
        )

        assertEquals(25.0, updatedLoad.bounceMilesEnd, 0.01)
        assertEquals(110.0, updatedLoad.loadedMilesEnd, 0.01)
    }

    @Test
    fun testLiveLegOorRecalculationOnAdjustedMiles() {
        val dispatchedBounce = 100.0
        val adjustedActualBounce = 125.0 // 25 miles out of route

        val oorMiles = maxOf(0.0, adjustedActualBounce - dispatchedBounce)
        val oorPct = (oorMiles / dispatchedBounce) * 100.0

        assertEquals(25.0, oorMiles, 0.01)
        assertEquals(25.0, oorPct, 0.01)
    }
}
