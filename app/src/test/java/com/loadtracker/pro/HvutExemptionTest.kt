package com.loadtracker.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.Month

class HvutExemptionTest {

    @Test
    fun test1_TaxYearStartDate_CalculatesJuly1stCorrectly() {
        val dateInAugust = LocalDate.of(2026, Month.AUGUST, 15)
        val taxYearStartAug = HvutCalculator.getCurrentTaxYearStartDate(dateInAugust)
        assertEquals(2026, taxYearStartAug.year)
        assertEquals(Month.JULY, taxYearStartAug.month)
        assertEquals(1, taxYearStartAug.dayOfMonth)

        val dateInMay = LocalDate.of(2026, Month.MAY, 10)
        val taxYearStartMay = HvutCalculator.getCurrentTaxYearStartDate(dateInMay)
        assertEquals(2025, taxYearStartMay.year)
        assertEquals(Month.JULY, taxYearStartMay.month)
        assertEquals(1, taxYearStartMay.dayOfMonth)
    }

    @Test
    fun test2_ExemptionThreshold_Evaluates5000MilesLimit() {
        val lowMiles = 3200.0
        val overMiles = 5400.0

        assertTrue("3200 miles should be exempt under 5,000 threshold", HvutCalculator.isExemptFromTax(lowMiles))
        assertFalse("5400 miles should exceed 5,000 exemption threshold", HvutCalculator.isExemptFromTax(overMiles))

        assertEquals(1800.0, HvutCalculator.calculateRemainingExemptMiles(lowMiles), 0.01)
        assertEquals(0.0, HvutCalculator.calculateRemainingExemptMiles(overMiles), 0.01)
    }

    @Test
    fun test3_FilingDeadline_CalculatesDaysRemainingUntilAugust31() {
        val today = LocalDate.of(2026, Month.AUGUST, 1)
        val daysLeft = HvutCalculator.calculateDaysUntilDeadline(today)
        assertEquals(30L, daysLeft)
    }
}
