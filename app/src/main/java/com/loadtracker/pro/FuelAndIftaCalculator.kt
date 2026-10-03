package com.loadtracker.pro

object FuelAndIftaCalculator {
    data class IftaSummary(
        val state: String,
        val totalGallonsPurchased: Double,
        val totalMilesTraveled: Double,
        val averageMpg: Double
    )

    fun calculateMpg(totalGallons: Double, totalMiles: Double): Double {
        if (totalGallons <= 0.0) return 0.0
        return totalMiles / totalGallons
    }

    fun calculatePricePerGallon(totalCost: Double, gallons: Double): Double {
        if (gallons <= 0.0) return 0.0
        return totalCost / gallons
    }
}
