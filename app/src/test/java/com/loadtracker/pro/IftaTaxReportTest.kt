package com.loadtracker.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IftaTaxReportTest {

    @Test
    fun test1_IftaCsvGeneration_ProducesFormattedQuarterlyReport() {
        val fuel1 = FuelEntry(
            id = 1,
            timestamp = 1700000000000L,
            gallons = 80.0,
            totalCost = 280.00,
            pricePerGallon = 3.50,
            state = "TX",
            stationName = "Love's",
            odometer = 120000.0
        )

        val breadcrumb1 = TripBreadcrumb(id = 1, proNumber = "52167364", latitude = 32.7767, longitude = -96.7970, timestamp = 1000000L)
        val breadcrumb2 = TripBreadcrumb(id = 2, proNumber = "52167364", latitude = 32.5251, longitude = -94.7403, timestamp = 1010000L)

        val csv = IftaTaxReportExporter.generateIftaCsv(
            quarterLabel = "Q3",
            fuelEntries = listOf(fuel1),
            breadcrumbs = listOf(breadcrumb1, breadcrumb2)
        )

        assertTrue("CSV header should be present", csv.contains("Quarter,State,Miles Driven,Gallons Pumped"))
        assertTrue("TX row should be present in CSV", csv.contains("\"TX\""))
        assertTrue("Q3 quarter label should be present", csv.contains("\"Q3\""))
    }
}
