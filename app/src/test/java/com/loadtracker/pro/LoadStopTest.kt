package com.loadtracker.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class LoadStopTest {

    @Test
    fun codecRoundTripsAddressesAndLegProgress() {
        val stops = listOf(
            LoadStop(
                name = "Dock A: East",
                address = "12 Main St; Suite 4",
                latitude = 32.1,
                longitude = -96.4,
                dispatchedLegMiles = 42.5,
                actualLegMiles = 48.0
            )
        )

        assertEquals(stops, LoadStopCodec.decode(LoadStopCodec.encode(stops)))
    }

    @Test
    fun recordsStopArrivalAndThenFinalConsigneeArrivalByLoadedLeg() {
        val original = loadWithStop()
        val atStop = original.recordLoadedArrival(47.0, 1_000L)
        val stop = LoadStopCodec.decode(atStop.stopOffsData).single()

        assertEquals("ACTIVE_STOP", atStop.tripState)
        assertEquals(1, atStop.currentStopIndex)
        assertEquals(47.0, stop.actualLegMiles!!, 0.001)
        assertEquals(
            5.0,
            (stop.actualLegMiles!! - stop.dispatchedLegMiles).coerceAtLeast(0.0),
            0.001
        )
        assertEquals(47.0, atStop.lastStopLoadedMiles, 0.001)
        assertEquals(30.0, atStop.plannedCurrentLoadedLegMiles(), 0.001)

        val atConsignee = atStop.recordLoadedArrival(82.0, 2_000L)
        assertEquals("ACTIVE_CONSIGNEE", atConsignee.tripState)
        val finalLegActual = atConsignee.loadedMilesEnd - atConsignee.lastStopLoadedMiles
        assertEquals(35.0, finalLegActual, 0.001)
        assertEquals(5.0, (finalLegActual - atConsignee.plannedCurrentLoadedLegMiles()).coerceAtLeast(0.0), 0.001)
        assertEquals(
            47.0,
            LoadStopCodec.decode(atConsignee.stopOffsData).single().actualLegMiles!!,
            0.001
        )
    }

    @Test
    fun parsesNumberedStopFieldsFromOcrText() {
        val stops = StopOffParser.parseRouteStops(
            """
                Stop Off 1 Name: North Yard
                Stop Off 1 Address: 12 Main St, Dallas, TX
                Stop Off 1 Leg Miles: 42
            """.trimIndent()
        )

        assertEquals(1, stops.size)
        assertEquals("North Yard", stops.single().name)
        assertEquals("12 Main St, Dallas, TX", stops.single().address)
        assertEquals("42", stops.single().dispatchedLegMiles)
        assertTrue(stops.single().toLoadStopOrNull() == null)
    }

    private fun loadWithStop(): CurrentLoad {
        val zone = ZoneId.of("America/Chicago")
        return CurrentLoad(
            proNumber = "MULTI-STOP",
            dispatchedBounceMiles = 10.0,
            dispatchedLoadedMiles = 72.0,
            bounceMilesStart = 0.0,
            bounceMilesEnd = 0.0,
            loadedMilesStart = 0.0,
            loadedMilesEnd = 0.0,
            percentageRate = 31.0,
            loadPay = 100.0,
            tarpType = "N",
            isPreTarped = false,
            pickupTimestamp = LocalDate.of(2026, 10, 5)
                .atStartOfDay(zone).toInstant().toEpochMilli(),
            isGoingHome = false,
            tripState = "ACTIVE_LOADED",
            stopOffsData = LoadStopCodec.encode(
                listOf(
                    LoadStop(
                        name = "North Yard",
                        address = "12 Main St",
                        latitude = 32.1,
                        longitude = -96.4,
                        dispatchedLegMiles = 42.0
                    )
                )
            )
        )
    }
}
