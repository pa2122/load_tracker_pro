package com.loadtracker.pro

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsigneeAddressPendingTest {
    private fun load(state: String, lat: Double?) = CurrentLoad(
        proNumber = "1", dispatchedBounceMiles = 0.0, dispatchedLoadedMiles = 0.0,
        bounceMilesStart = 0.0, bounceMilesEnd = 0.0, loadedMilesStart = 0.0, loadedMilesEnd = 0.0,
        percentageRate = 31.0, loadPay = 0.0, tarpType = "N", isPreTarped = false,
        pickupTimestamp = 0L, isGoingHome = false, tripState = state, consigneeLat = lat
    )

    @Test
    fun activeLoadWithoutCoordinatesIsPending() {
        assertTrue(load("ACTIVE_LOADED", null).isConsigneeAddressPending)
        assertTrue(load("ACTIVE_CONSIGNEE", null).isConsigneeAddressPending)
    }

    @Test
    fun verifiedOrCompletedLoadIsNotPending() {
        assertFalse(load("ACTIVE_LOADED", 32.0).isConsigneeAddressPending)
        assertFalse(load("COMPLETED", null).isConsigneeAddressPending)
    }
}
