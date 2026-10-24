package com.loadtracker.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrackingNotificationActionTest {

    @Test
    fun `notification action follows active trip state`() {
        assertEquals(
            TrackingNotificationAction.ARRIVE_SHIPPER,
            TrackingNotificationAction.forTripState("ACTIVE_BOUNCE")
        )
        assertEquals(
            TrackingNotificationAction.DEPART_SHIPPER,
            TrackingNotificationAction.forTripState("ACTIVE_SHIPPER")
        )
        assertEquals(
            TrackingNotificationAction.ARRIVE_DESTINATION,
            TrackingNotificationAction.forTripState("ACTIVE_LOADED")
        )
        assertEquals(
            TrackingNotificationAction.DEPART_STOP,
            TrackingNotificationAction.forTripState("ACTIVE_STOP")
        )
        assertEquals(
            TrackingNotificationAction.OPEN_TO_COMPLETE,
            TrackingNotificationAction.forTripState("ACTIVE_CONSIGNEE")
        )
    }

    @Test
    fun `notification action is hidden when there is no active trip step`() {
        assertNull(TrackingNotificationAction.forTripState("COMPLETED"))
        assertNull(TrackingNotificationAction.forTripState("NOT_STARTED"))
    }
}
