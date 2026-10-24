package com.loadtracker.pro

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackingServiceGeofenceTest {

    @Test
    fun `arrival triggers only after a dwell time when vehicle is stopped near the stop`() {
        val (triggered, nextStart) = TrackingService.shouldTriggerArrival(
            distanceMeters = 30.0,
            speedMps = 0.0,
            candidateStartMs = 1_000_000L - TrackingService.ARRIVAL_DWELL_MS,
            nowMs = 1_000_000L,
        )

        assertTrue("Stopped vehicle near facility should trigger after the dwell window", triggered)
        assertTrue("Candidate timer should reset after arrival", nextStart == null)
    }

    @Test
    fun `passing by a stop at highway speed does not trigger arrival`() {
        val (triggered, _) = TrackingService.shouldTriggerArrival(
            distanceMeters = 30.0,
            speedMps = 8.0,
            candidateStartMs = 1_000_000L - TrackingService.ARRIVAL_DWELL_MS,
            nowMs = 1_000_000L,
        )

        assertFalse("Passing by the stop at speed should not trigger arrival", triggered)
    }
}
