package com.loadtracker.pro

object OwnerOpBusinessCalculator {

    data class BusinessSummary(
        val totalGrossRevenue: Double,
        val totalDriverTakeHome: Double,
        val totalFuelAndDefExpenses: Double,
        val totalMilesDriven: Double,
        val netProfit: Double,
        val profitMarginPercentage: Double,
        val costPerMile: Double
    )

    private fun Double.toCents(): Long = Math.round(this * 100.0)
    private fun Long.toDollars(): Double = this / 100.0

    /**
     * Calculates complete Owner-Operator P&L metrics and Cost-Per-Mile.
     */
    fun calculateBusinessSummary(
        loads: List<CurrentLoad>,
        fuelEntries: List<FuelEntry>,
        lumberRate: Double = 50.0,
        steelRate: Double = 30.0
    ): BusinessSummary {
        val grossRevenue = loads.sumOf { it.loadPay }

        var totalNetPayCents = 0L
        var totalMiles = 0.0

        loads.forEach { load ->
            val grossCents = load.loadPay.toCents()
            val cutCents = Math.round(grossCents * (load.percentageRate / 100.0))

            var tarp = when (load.tarpType) {
                "L" -> lumberRate
                "S" -> steelRate
                else -> 0.0
            }
            if (load.isPreTarped) tarp /= 2.0
            val tarpCents = tarp.toCents()

            val bounceBonus = if (load.dispatchedBounceMiles >= 150.0) load.dispatchedBounceMiles * 0.20 else 0.0
            val bonusCents = bounceBonus.toCents()

            val trainerCents = load.trainerPayRate.toCents()

            totalNetPayCents += (cutCents + tarpCents + bonusCents + trainerCents)

            val actB = load.bounceMilesEnd - load.bounceMilesStart
            val actL = load.loadedMilesEnd - load.loadedMilesStart
            val actual = actB + actL
            val disp = load.dispatchedBounceMiles + load.dispatchedLoadedMiles
            totalMiles += if (actual > 0) actual else disp
        }

        val fuelAndDefExpenses = fuelEntries.sumOf { it.totalCost + it.defCost }
        val driverTakeHome = totalNetPayCents.toDollars()

        // Net Profit = Gross Revenue - Fuel & DEF Expenses
        val netProfit = grossRevenue - fuelAndDefExpenses
        val marginPct = if (grossRevenue > 0) (netProfit / grossRevenue) * 100.0 else 0.0
        val cpm = if (totalMiles > 0) fuelAndDefExpenses / totalMiles else 0.0

        return BusinessSummary(
            totalGrossRevenue = grossRevenue,
            totalDriverTakeHome = driverTakeHome,
            totalFuelAndDefExpenses = fuelAndDefExpenses,
            totalMilesDriven = totalMiles,
            netProfit = netProfit,
            profitMarginPercentage = marginPct,
            costPerMile = cpm
        )
    }
}
