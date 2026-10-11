package com.loadtracker.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class OcrDateParserTest {
    private val zone = ZoneId.of("America/Chicago")

    @Test
    fun extractsLabeledDeliveryDateAsLocalDay() {
        val timestamp = OcrDateParser.extractDeliveryDateMillis(
            "Pickup Date: 10/08/2026\nDelivery Date: 10/09/2026",
            zone
        )

        assertEquals(
            LocalDate.of(2026, 10, 9),
            java.time.Instant.ofEpochMilli(timestamp!!).atZone(zone).toLocalDate()
        )
    }

    class OcrTarpParserTest {
        @Test
        fun preTarpMarkerDoesNotAssumeFourOrEightFootDrop() {
            assertEquals(
                OcrTarpInfo(tarpType = null, isPreTarped = true),
                OcrTarpParser.parse("Tarp: P")
            )
        }

        @Test
        fun mapsKnownTarpCodesToDropSizes() {
            assertEquals(
                OcrTarpInfo(tarpType = "4' Drop", isPreTarped = null),
                OcrTarpParser.parse("Tarp: S")
            )
            assertEquals(
                OcrTarpInfo(tarpType = "8' Drop", isPreTarped = null),
                OcrTarpParser.parse("Tarp: L")
            )
        }

        @Test
        fun ignoresUnrelatedPCharacters() {
            assertNull(OcrTarpParser.parse("Parking: YES\nLoad Weight: 47761"))
        }
    }

    @Test
    fun doesNotTreatDeliveryAppointmentAsActualDeliveryDate() {
        assertNull(
            OcrDateParser.extractDeliveryDateMillis(
                "Delivery Appointment: 10/09/2026 08:00 AM",
                zone
            )
        )
    }

    @Test
    fun suggestsFinalDropoffAppointmentFromDispatchScreenshot() {
        val timestamp = OcrDateParser.extractDeliveryDateMillis(
            """
                Stop Type: Origin
                B 10/8/2026 2:00:00 PM
                Stop Type: Final Dropoff
                B 10/12/2026 4:00:00 PM
            """.trimIndent(),
            zone
        )

        assertEquals(
            LocalDate.of(2026, 10, 12),
            java.time.Instant.ofEpochMilli(timestamp!!).atZone(zone).toLocalDate()
        )
    }

    @Test
    fun convertsDatePickerDaysWithoutChangingLocalCalendarDate() {
        val pickerDate = LocalDate.of(2026, 10, 9)
            .atStartOfDay(java.time.ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
        val timestamp = OcrDateParser.timestampForDatePickerMillis(pickerDate, zone)

        assertEquals(
            pickerDate,
            OcrDateParser.datePickerMillisForTimestamp(timestamp, zone)
        )
    }
}
