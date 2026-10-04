package com.loadtracker.pro

object PayrollEngine {

    data class NetPayBreakdown(
        val grossRevenueCents: Long,
        val driverCutCents: Long,
        val tarpPayCents: Long,
        val bounceBonusCents: Long,
        val trainerPayCents: Long,
        val extraStopPayCents: Long,
        val totalNetPayCents: Long
    ) {
        val grossRevenueDollars: Double get() = grossRevenueCents.toDollars()
        val driverCutDollars: Double get() = driverCutCents.toDollars()
        val tarpPayDollars: Double get() = tarpPayCents.toDollars()
        val bounceBonusDollars: Double get() = bounceBonusCents.toDollars()
        val trainerPayDollars: Double get() = trainerPayCents.toDollars()
        val extraStopPayDollars: Double get() = extraStopPayCents.toDollars()
        val totalNetPayDollars: Double get() = totalNetPayCents.toDollars()
    }

    fun Double.toCents(): Long = Math.round(this * 100.0)
    fun Long.toDollars(): Double = this / 100.0

    /**
     * Single source of truth for calculating driver net pay breakdown for a load.
     */
    fun calculateLoadNetPay(
        load: CurrentLoad,
        lumberRate: Double = 50.0,
        steelRate: Double = 30.0,
        extraStopRate: Double = 0.0
    ): NetPayBreakdown {
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

        val trainerCents = if (load.isTrainingWeek) load.trainerPayRate.toCents() else 0L
        val extraStopCents = extraStopRate.toCents()

        val totalCents = cutCents + tarpCents + bonusCents + trainerCents + extraStopCents

        return NetPayBreakdown(
            grossRevenueCents = grossCents,
            driverCutCents = cutCents,
            tarpPayCents = tarpCents,
            bounceBonusCents = bonusCents,
            trainerPayCents = trainerCents,
            extraStopPayCents = extraStopCents,
            totalNetPayCents = totalCents
        )
    }

    /**
     * Calculates total net pay for a list of completed loads.
     */
    fun calculateTotalNetPay(
        loads: List<CurrentLoad>,
        lumberRate: Double = 50.0,
        steelRate: Double = 30.0
    ): Double {
        var totalCents = 0L
        loads.forEach { load ->
            totalCents += calculateLoadNetPay(load, lumberRate, steelRate).totalNetPayCents
        }
        return totalCents.toDollars()
    }
}
