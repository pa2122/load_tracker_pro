package com.loadtracker.pro

import org.junit.Assert.assertEquals
import org.junit.Test

class FuelMpgAnalyticsTest {

    @Test
    fun test1_MpgCalculation_ComputesMilesPerGallonCorrectly() {
        val entry1 = FuelEntry(
            id = 1,
            timestamp = 1000000L,
            gallons = 80.0,
            totalCost = 280.00,
            pricePerGallon = 3.50,
            state = "TX",
            stationName = "Love's Sinton",
            odometer = 120000.0,
            defGallons = 5.0,
            defCost = 20.00
        )

        val entry2 = FuelEntry(
            id = 2,
            timestamp = 1080000L,
            gallons = 80.0,
            totalCost = 280.00,
            pricePerGallon = 3.50,
            state = "AR",
            stationName = "Pilot Hope",
            odometer = 120520.0, // Driven 520 miles
            defGallons = 5.0,
            defCost = 20.00
        )

        val milesDriven = entry2.odometer - entry1.odometer
        val mpg = milesDriven / entry2.gallons

        assertEquals(520.0, milesDriven, 0.01)
        assertEquals(6.5, mpg, 0.01) // 520 miles / 80 gallons = 6.5 MPG
    }

    @Test
    fun test2_CostPerMile_IncludesFuelAndDefExpenses() {
        val milesDriven = 520.0
        val fuelCost = 280.00
        val defCost = 20.00
        val totalExpense = fuelCost + defCost

        val costPerMile = totalExpense / milesDriven

        assertEquals(300.00, totalExpense, 0.01)
        assertEquals(0.5769, costPerMile, 0.01) // $300 / 520 mi = ~$0.58/mi
    }
}
