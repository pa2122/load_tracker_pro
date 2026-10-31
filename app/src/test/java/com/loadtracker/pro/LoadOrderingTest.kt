package com.loadtracker.pro

import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class LoadOrderingTest {
    private val zone = ZoneId.of("America/Chicago")

    private fun millis(y: Int, m: Int, d: Int, h: Int = 9) =
        LocalDateTime.of(y, m, d, h, 0).atZone(zone).toInstant().toEpochMilli()

    private fun load(pro: String, pickup: Long, delivered: Long?) = CurrentLoad(
        proNumber = pro, dispatchedBounceMiles = 0.0, dispatchedLoadedMiles = 0.0,
        bounceMilesStart = 0.0, bounceMilesEnd = 0.0, loadedMilesStart = 0.0, loadedMilesEnd = 0.0,
        percentageRate = 31.0, loadPay = 0.0, tarpType = "N", isPreTarped = false,
        pickupTimestamp = pickup, isGoingHome = false, tripState = "COMPLETED",
        deliveryTimestamp = delivered
    )

    @Test
    fun newestDeliveryComesFirstEvenWhenPickedUpEarlier() {
        val thursdayPickupMondayDelivery = load("A", millis(2026, 10, 15), millis(2026, 10, 19))
        val wednesdayPickupThursdayDelivery = load("B", millis(2026, 10, 21), millis(2026, 10, 22))
        val sorted = listOf(wednesdayPickupThursdayDelivery, thursdayPickupMondayDelivery).sortedByNewestDelivery()
        assertEquals(listOf("B", "A"), sorted.map { it.proNumber })
    }

    @Test
    fun loadsWithoutDeliveryFallBackToPickup() {
        val old = load("OLD", millis(2026, 10, 1), null)
        val recent = load("NEW", millis(2026, 10, 5), millis(2026, 10, 6))
        assertEquals(listOf("NEW", "OLD"), listOf(old, recent).sortedByNewestDelivery().map { it.proNumber })
    }

    @Test
    fun formatsPickupAndDelivery() {
        val l = load("A", millis(2026, 10, 15), millis(2026, 10, 19))
        assertEquals("Picked up Thu 10/15 → Delivered Mon 10/19", formatLoadDates(l, zone))
    }
}
