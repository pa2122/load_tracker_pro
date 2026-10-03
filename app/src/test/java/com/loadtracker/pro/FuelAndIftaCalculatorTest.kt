package com.loadtracker.pro

import org.junit.Assert.assertEquals
import org.junit.Test

class FuelAndIftaCalculatorTest {

    @Test
    fun testCalculateMpg() {
        val mpg = FuelAndIftaCalculator.calculateMpg(100.0, 650.0)
        assertEquals(6.5, mpg, 0.01)

        val zeroGallonsMpg = FuelAndIftaCalculator.calculateMpg(0.0, 500.0)
        assertEquals(0.0, zeroGallonsMpg, 0.01)
    }

    @Test
    fun testCalculatePricePerGallon() {
        val ppg = FuelAndIftaCalculator.calculatePricePerGallon(450.00, 120.0)
        assertEquals(3.75, ppg, 0.01)

        val zeroGallonsPpg = FuelAndIftaCalculator.calculatePricePerGallon(100.00, 0.0)
        assertEquals(0.0, zeroGallonsPpg, 0.01)
    }
}
