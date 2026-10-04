package com.loadtracker.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class WearDataSyncTest {

    @Test
    fun test1_WearDataSyncPayload_CreatesValidDataMapPayload() {
        val payload = WearableDataSyncManager.WearTripStatePayload(
            proNumber = "52167364",
            tripState = "ACTIVE_LOADED",
            bounceMiles = 155.0,
            loadedMiles = 482.0,
            dockArrivalTime = 1700000000000L
        )

        assertNotNull("Payload should not be null", payload)
        assertEquals("52167364", payload.proNumber)
        assertEquals("ACTIVE_LOADED", payload.tripState)
        assertEquals(155.0, payload.bounceMiles, 0.01)
        assertEquals(482.0, payload.loadedMiles, 0.01)
        assertEquals(1700000000000L, payload.dockArrivalTime)
    }

    @Test
    fun test2_WearWristAction_VerifiesWristActionCommandConstants() {
        assertEquals("ARRIVE_SHIPPER", WearableDataSyncManager.ACTION_ARRIVE_SHIPPER)
        assertEquals("DEPART_SHIPPER", WearableDataSyncManager.ACTION_DEPART_SHIPPER)
        assertEquals("ARRIVE_CONSIGNEE", WearableDataSyncManager.ACTION_ARRIVE_CONSIGNEE)
        assertEquals("COMPLETE_LOAD", WearableDataSyncManager.ACTION_COMPLETE_LOAD)
    }
}
