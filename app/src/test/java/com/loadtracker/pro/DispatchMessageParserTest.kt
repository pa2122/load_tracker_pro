package com.loadtracker.pro

import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DispatchMessageParserTest {
    private val zone = ZoneId.of("America/Chicago")

    private val tmcMessage = """
        Stop Type: Origin
        NATIONAL GYPSUM
        832 COUNTY ROAD 311
        ROTAN, TX
        B 10/20/2026 11:00:00 PM
        Instructions:
        DROP & HOOK SHIPPER. CHECK IN AT OFFICE
        Directions:
        FROM TOWN GO NORTH
        Parking:
        ON SITE
        Stop Type: Final Dropoff
        FBM
        801 MARIO CT
        LEWISVILLE, TX
        B 10/21/2026 2:30:00 PM
        Instructions:
        CALL AHEAD
        Pro Number: 65219909
        Loaded Miles: 228
        Bounce Miles: 111
        Gross: ${'$'}760.22
        Tarp: P
    """.trimIndent()

    @Test
    fun parsesTmcStopTypeMessage() {
        val parsed = DispatchMessageParser.parse(tmcMessage, zone)

        assertEquals("65219909", parsed.proNumber)
        assertEquals("228", parsed.dispatchedLoadedMiles)
        assertEquals("111", parsed.dispatchedBounceMiles)
        assertEquals("760.22", parsed.loadPay)
        assertEquals("NATIONAL GYPSUM\n832 COUNTY ROAD 311\nROTAN, TX", parsed.shipperText)
        assertEquals("FBM\n801 MARIO CT\nLEWISVILLE, TX", parsed.consigneeText)
        assertEquals(true, parsed.tarp?.isPreTarped)
        assertEquals("BEFORE", parsed.pickupAppointment?.type)
        assertTrue(parsed.pickupDateMillis != null)
        assertTrue(parsed.deliveryAppointment != null)
    }

    @Test
    fun instructionTextMentioningShipperDoesNotChangeFacility() {
        val parsed = DispatchMessageParser.parse(tmcMessage, zone)

        assertFalse(parsed.consigneeText.contains("SHIPPER", ignoreCase = true))
        assertTrue(parsed.shipperNotes.instructions.contains("DROP & HOOK SHIPPER"))
        assertEquals("CALL AHEAD", parsed.consigneeNotes.instructions)
        assertEquals("ON SITE", parsed.shipperNotes.parking)
    }

    @Test
    fun dropoffOnlyScreenshotHasNoPickupDate() {
        val text = """
            Stop Type: Final Dropoff
            WHITE CAP
            291 SALEM STREET
            WOBURN, MA
            4/2/2026 12:00:00 AM
            Pro Number: 90166395
        """.trimIndent()
        val parsed = DispatchMessageParser.parse(text, zone)

        assertEquals(null, parsed.pickupDateMillis)
        assertTrue(parsed.deliveryAppointment != null)
    }

    @Test
    fun splitScreenshotsWithSamePro_areMerged() {
        val top = ScreenshotBatchParser.parse(
            "top.png",
            "Stop Type: Origin\nHOME DEPOT #5861\n299 CHAPEL RD\nS WINDSOR, CT\nB 4/2/2026 11:59:00 PM\nPro Number: 90166305",
            "30"
        )
        val bottom = ScreenshotBatchParser.parse(
            "bottom.png",
            "Pro Number: 90166305\nLoaded Miles: 382\nBounce Miles: 97\nGross: 1066.28\nTarp: P",
            "30"
        )

        val merged = ScreenshotBatchParser.mergeSplitScreenshots(listOf(top, bottom)).single()

        assertEquals("04/02/2026", merged.pickupDate)
        assertEquals("1066.28", merged.grossPay)
        assertEquals("382", merged.loadedMiles)
        assertEquals("HOME DEPOT #5861\n299 CHAPEL RD\nS WINDSOR, CT", merged.shipperName)
    }

    @Test
    fun cutOffScreenshotReportsMissingFields() {
        val text = """
            Stop Type: Origin
            WEST FRASER INC
            1345 US 82
            NEW BOSTON, TX
            B 3/4/2026 10:00:00 PM
            Stop Type: Final Dropoff
            UFP NEW WAVERLY LLC
            146 FM 2793 ROAD BLDG B
            HUNTSVILLE, TX
            @ 3/5/2026 12:00:00 PM
            Pro Number: 52127530
        """.trimIndent()

        assertEquals(
            listOf(DispatchField.PAY, DispatchField.LOADED_MILES, DispatchField.BOUNCE_MILES),
            DispatchMessageParser.parse(text, zone).missingFields
        )
        assertTrue(DispatchMessageParser.parse(tmcMessage, zone).missingFields.isEmpty())
    }

    @Test
    fun batchDraftMissingFieldsBlockImport() {
        val draft = ScreenshotBatchParser.parse("a.png", "Pro Number: 90166395\nStop Type: Final Dropoff\nWHITE CAP\n291 SALEM STREET\nWOBURN, MA", "30")

        assertTrue(DispatchField.PICKUP_DATE in draft.missingFields())
        assertFalse(draft.isReadyToImport(emptySet(), mapOf("90166395" to 1)))
    }

    @Test
    fun batchParserUsesSharedParser() {
        val draft = ScreenshotBatchParser.parse("a.png", tmcMessage, "30")

        assertEquals("65219909", draft.proNumber)
        assertEquals("10/20/2026", draft.pickupDate)
        assertEquals("NATIONAL GYPSUM\n832 COUNTY ROAD 311\nROTAN, TX", draft.shipperName)
        assertEquals("760.22", draft.grossPay)
    }
}
