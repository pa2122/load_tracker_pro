package com.loadtracker.pro

import org.junit.Assert.assertEquals
import org.junit.Test

class OwnerOpBusinessSuiteTest {

    @Test
    fun test1_PandLCalculation_ComputesRevenueExpensesAndMarginCorrectly() {
        val load1 = CurrentLoad(
            proNumber = "52167364",
            dispatchedBounceMiles = 155.0,
            dispatchedLoadedMiles = 968.0,
            bounceMilesStart = 0.0,
            bounceMilesEnd = 155.0,
            loadedMilesStart = 0.0,
            loadedMilesEnd = 968.0,
            percentageRate = 31.0,
            loadPay = 2335.03,
            tarpType = "S",
            isPreTarped = false,
            pickupTimestamp = 1700000000000L,
            isGoingHome = false,
            tripState = "COMPLETED"
        )

        val fuel1 = FuelEntry(
            id = 1,
            timestamp = 1700000000000L,
            gallons = 80.0,
            totalCost = 280.00,
            pricePerGallon = 3.50,
            state = "TX",
            stationName = "Love's",
            odometer = 120000.0,
            defGallons = 5.0,
            defCost = 20.00
        )

        val summary = OwnerOpBusinessCalculator.calculateBusinessSummary(
            loads = listOf(load1),
            fuelEntries = listOf(fuel1)
        )

        assertEquals(2335.03, summary.totalGrossRevenue, 0.01)
        assertEquals(300.00, summary.totalFuelAndDefExpenses, 0.01) // $280 fuel + $20 DEF
        assertEquals(2035.03, summary.netProfit, 0.01) // $2,335.03 - $300.00 = $2,035.03
        assertEquals(87.15, summary.profitMarginPercentage, 0.01) // ($2,035.03 / $2,335.03) * 100 = ~87.15%
        assertEquals(0.2671, summary.costPerMile, 0.01) // $300.00 / 1,123 miles = ~$0.27/mi
    }
}
