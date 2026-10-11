package com.loadtracker.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenshotBatchParserTest {

    @Test
    fun parsesHistoricalLoadFieldsAndPreTarpMarker() {
        val draft = ScreenshotBatchParser.parse(
            fileName = "dispatch.png",
            rawText = """
                PRO Number: 12345678
                Pickup Date: 10/05/2026
                Delivery Date: 10/06/2026
                Gross Pay: $2,750.00
                Bounce Miles: 150
                Loaded Miles: 1,025
                Stop Offs: 2
                Tarp: P
                Shipper: North Yard
                Consignee: South Yard
                Going Home: Y
            """.trimIndent(),
            defaultPercentage = "31.0"
        )

        assertEquals("12345678", draft.proNumber)
        assertEquals("10/05/2026", draft.pickupDate)
        assertEquals("10/06/2026", draft.deliveryDate)
        assertEquals("2750.00", draft.grossPay)
        assertEquals("150", draft.bounceMiles)
        assertEquals("1025", draft.loadedMiles)
        assertEquals("2", draft.extraStopCount)
        assertEquals("31.0", draft.percentageRate)
        assertEquals("None", draft.tarpType)
        assertTrue(draft.isPreTarped)
        assertEquals("North Yard", draft.shipperName)
        assertEquals("South Yard", draft.consigneeName)
        assertTrue(draft.isGoingHome)
    }

    @Test
    fun doesNotUseDeliveryDateAsPickupDate() {
        val draft = ScreenshotBatchParser.parse(
            fileName = "dispatch.png",
            rawText = "PRO: 12345678\nDelivery Date: 10/06/2026",
            defaultPercentage = "31"
        )

        assertEquals("", draft.pickupDate)
        assertEquals("0", draft.extraStopCount)
        assertFalse(draft.isPreTarped)
    }

    @Test
    fun parsesStopOffsEvenWhenTheCountIsOnTheNextLine() {
        assertEquals(3, requireNotNull(StopOffParser.parseCount("Stop Offs\n3")))
        assertNull(StopOffParser.parseCount("No stops listed"))
    }

    @Test
    fun parsesNumberedStopOffDestinationFieldsFromScreenshotText() {
        val stop = StopOffParser.parseRouteStops(
            """
                Stop Offs: 1
                Stop Off 1 Name: North Yard
                Stop Off 1 Address: 12 Main St, Dallas, TX
                Stop Off 1 Dispatched Leg Miles: 42
            """.trimIndent()
        ).single()

        assertEquals("North Yard", stop.name)
        assertEquals("12 Main St, Dallas, TX", stop.address)
        assertEquals("42", stop.dispatchedLegMiles)
    }
}
