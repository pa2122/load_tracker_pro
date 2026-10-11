package com.loadtracker.pro

import java.time.LocalDate
import java.time.ZoneId
import java.time.Instant
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

object PayrollCalculator {

    fun calculateWeekSummary(
        loads: List<CurrentLoad>,
        targetWeekStart: LocalDate,
        lumberRate: Double,
        steelRate: Double,
        isTrainingGlobal: Boolean,
        trainerRate: Double
    ): WeekSummary {
        var totalPayCents = 0L
        var totalActualMiles = 0.0
        var totalDispatchedMiles = 0.0
        var totalOutOfRouteMiles = 0.0

        val weekLoads = loads.filter { it.payrollWeekStartDate() == targetWeekStart }

        weekLoads.forEach { load ->
            val grossCents = load.loadPay.toCents()
            val baseSplitCents = Math.round(grossCents * (load.percentageRate / 100.0))

            var tarpPay = when (load.tarpType) {
                "L" -> lumberRate
                "S" -> steelRate
                else -> 0.0
            }
            if (load.isPreTarped) tarpPay /= 2.0

            val bounceBonus = if (load.dispatchedBounceMiles >= 150.0) {
                load.dispatchedBounceMiles * 0.20
            } else {
                0.0
            }
            totalPayCents += baseSplitCents + tarpPay.toCents() + bounceBonus.toCents()

            val actualMiles = (load.bounceMilesEnd - load.bounceMilesStart) +
                    (load.loadedMilesEnd - load.loadedMilesStart)
            val dispatchedMiles = load.dispatchedBounceMiles + load.dispatchedLoadedMiles
            totalActualMiles += actualMiles
            totalDispatchedMiles += dispatchedMiles

            if (!load.isGoingHome) {
                val outOfRouteMiles = actualMiles - dispatchedMiles
                if (outOfRouteMiles > 0.0) totalOutOfRouteMiles += outOfRouteMiles
            }
        }

        val snapshotTrainerPay = weekLoads.maxOfOrNull { it.trainerPayRate }
            ?.takeIf { it > 0.0 }
        val weekTrainerPay = snapshotTrainerPay
            ?: if (weekLoads.any { it.isTrainingWeek } || isTrainingGlobal) trainerRate else 0.0
        totalPayCents += weekTrainerPay.toCents()

        return WeekSummary(
            weeklyPay = totalPayCents.toDollars(),
            weeklyMilesDriven = totalActualMiles,
            weeklyOutOfRoute = totalOutOfRouteMiles,
            weeklyOorPercentage = if (totalDispatchedMiles > 0.0) {
                (totalOutOfRouteMiles / totalDispatchedMiles) * 100.0
            } else {
                0.0
            }
        )
    }
}

fun getPayrollWeekStartDate(
    timestamp: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): LocalDate = Instant.ofEpochMilli(timestamp)
    .atZone(zoneId)
    .toLocalDate()
    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

fun CurrentLoad.payrollWeekStartDate(
    zoneId: ZoneId = ZoneId.systemDefault()
): LocalDate? {
    if (tripState != "COMPLETED") return null
    // Older completed loads predate deliveryTimestamp; retain their totals using pickup date.
    return getPayrollWeekStartDate(deliveryTimestamp ?: pickupTimestamp, zoneId)
}
