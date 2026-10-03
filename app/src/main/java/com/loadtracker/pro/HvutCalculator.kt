package com.loadtracker.pro

import java.time.LocalDate
import java.time.Month
import java.time.temporal.ChronoUnit

object HvutCalculator {

    const val EXEMPTION_THRESHOLD_MILES = 5000.0

    /**
     * Determines the current IRS HVUT Tax Year start date (July 1st).
     * Tax year runs July 1 to June 30 of following year.
     */
    fun getCurrentTaxYearStartDate(now: LocalDate = LocalDate.now()): LocalDate {
        return if (now.month.value >= Month.JULY.value) {
            LocalDate.of(now.year, Month.JULY, 1)
        } else {
            LocalDate.of(now.year - 1, Month.JULY, 1)
        }
    }

    /**
     * Determines the annual IRS Form 2290 filing deadline (August 31st).
     */
    fun getFilingDeadline(now: LocalDate = LocalDate.now()): LocalDate {
        val taxYearStart = getCurrentTaxYearStartDate(now)
        return LocalDate.of(taxYearStart.year, Month.AUGUST, 31)
    }

    /**
     * Calculates days remaining until the August 31 filing deadline.
     */
    fun calculateDaysUntilDeadline(now: LocalDate = LocalDate.now()): Long {
        val deadline = getFilingDeadline(now)
        return ChronoUnit.DAYS.between(now, deadline)
    }

    /**
     * Evaluates 5,000-mile exemption status based on annual miles driven.
     */
    fun isExemptFromTax(annualMiles: Double): Boolean {
        return annualMiles <= EXEMPTION_THRESHOLD_MILES
    }

    /**
     * Calculates remaining miles before reaching 5,000-mile taxable threshold.
     */
    fun calculateRemainingExemptMiles(annualMiles: Double): Double {
        return maxOf(0.0, EXEMPTION_THRESHOLD_MILES - annualMiles)
    }
}
