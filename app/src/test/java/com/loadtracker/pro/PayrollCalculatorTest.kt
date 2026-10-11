package com.loadtracker.pro

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class PayrollCalculatorTest {

    private val targetWeekStart = LocalDate.of(2026, 10, 5)
    private val payrollZone = ZoneId.of("America/Chicago")

    @Test
    fun calculatesWeeklyPayAndMileageFromProductionLoadData() {
        val load = load(
            proNumber = "WEEK-LOAD",
            pickupDate = LocalDate.of(2026, 10, 7),
            deliveryDate = LocalDate.of(2026, 10, 9),
            dispatchedBounceMiles = 155.0,
            dispatchedLoadedMiles = 968.0,
            bounceMilesStart = 10.0,
            bounceMilesEnd = 170.0,
            loadedMilesStart = 20.0,
            loadedMilesEnd = 1000.0,
            percentageRate = 31.0,
            loadPay = 2335.03,
            tarpType = "S",
            trainerPayRate = 200.0
        )

        val summary = PayrollCalculator.calculateWeekSummary(
            loads = listOf(load),
            targetWeekStart = targetWeekStart,
            lumberRate = 50.0,
            steelRate = 30.0,
            isTrainingGlobal = false,
            trainerRate = 250.0
        )

        assertEquals(984.86, summary.weeklyPay, 0.001)
        assertEquals(1140.0, summary.weeklyMilesDriven, 0.001)
        assertEquals(17.0, summary.weeklyOutOfRoute, 0.001)
        assertEquals((17.0 / 1123.0) * 100.0, summary.weeklyOorPercentage, 0.001)
    }

    @Test
    fun excludesLoadsOutsideSelectedPayWeekAndGoingHomeMilesFromOutOfRoute() {
        val inWeekLoad = load(
            proNumber = "HOME-LOAD",
            pickupDate = LocalDate.of(2026, 10, 8),
            deliveryDate = LocalDate.of(2026, 10, 9),
            dispatchedBounceMiles = 150.0,
            dispatchedLoadedMiles = 500.0,
            bounceMilesStart = 0.0,
            bounceMilesEnd = 160.0,
            loadedMilesStart = 0.0,
            loadedMilesEnd = 500.0,
            percentageRate = 20.0,
            loadPay = 1000.0,
            isGoingHome = true
        )
        val nextWeekLoad = load(
            proNumber = "NEXT-WEEK",
            pickupDate = LocalDate.of(2026, 10, 9),
            deliveryDate = LocalDate.of(2026, 10, 12),
            loadPay = 5000.0,
            dispatchedBounceMiles = 200.0
        )

        val summary = PayrollCalculator.calculateWeekSummary(
            loads = listOf(inWeekLoad, nextWeekLoad),
            targetWeekStart = targetWeekStart,
            lumberRate = 50.0,
            steelRate = 30.0,
            isTrainingGlobal = false,
            trainerRate = 200.0
        )

        assertEquals(230.0, summary.weeklyPay, 0.001)
        assertEquals(660.0, summary.weeklyMilesDriven, 0.001)
        assertEquals(0.0, summary.weeklyOutOfRoute, 0.001)
        assertEquals(0.0, summary.weeklyOorPercentage, 0.001)
    }

    @Test
    fun usesTrainingLoadSnapshotBeforeGlobalTrainerRate() {
        val load = load(
            proNumber = "TRAINING-LOAD",
            pickupDate = LocalDate.of(2026, 10, 7),
            deliveryDate = LocalDate.of(2026, 10, 9),
            isTrainingWeek = true,
            trainerPayRate = 125.75
        )

        val summary = PayrollCalculator.calculateWeekSummary(
            loads = listOf(load),
            targetWeekStart = targetWeekStart,
            lumberRate = 0.0,
            steelRate = 0.0,
            isTrainingGlobal = true,
            trainerRate = 200.0
        )

        assertEquals(125.75, summary.weeklyPay, 0.001)
    }

    @Test
    fun includesExtraStopPayAtConfiguredRatePerStopOff() {
        val load = load(
            proNumber = "EXTRA-STOPS",
            pickupDate = LocalDate.of(2026, 10, 7),
            percentageRate = 100.0,
            loadPay = 100.0,
            extraStopCount = 2
        )

        val summary = PayrollCalculator.calculateWeekSummary(
            loads = listOf(load),
            targetWeekStart = targetWeekStart,
            lumberRate = 0.0,
            steelRate = 0.0,
            extraStopRate = 25.0,
            isTrainingGlobal = false,
            trainerRate = 0.0
        )

        assertEquals(150.0, summary.weeklyPay, 0.001)
    }

    @Test
    fun assignsLoadsToDeliveryWeekNotPickupWeek() {
        val thursdayPickupFridayDelivery = load(
            proNumber = "THU-FRI",
            pickupDate = LocalDate.of(2026, 10, 8),
            deliveryDate = LocalDate.of(2026, 10, 9),
            percentageRate = 100.0,
            loadPay = 100.0
        )
        val fridaySameDayDelivery = load(
            proNumber = "FRI-FRI",
            pickupDate = LocalDate.of(2026, 10, 9),
            deliveryDate = LocalDate.of(2026, 10, 9),
            percentageRate = 100.0,
            loadPay = 200.0
        )
        val fridayToMondayDelivery = load(
            proNumber = "FRI-MON",
            pickupDate = LocalDate.of(2026, 10, 9),
            deliveryDate = LocalDate.of(2026, 10, 12),
            percentageRate = 100.0,
            loadPay = 400.0
        )

        val summary = PayrollCalculator.calculateWeekSummary(
            loads = listOf(
                thursdayPickupFridayDelivery,
                fridaySameDayDelivery,
                fridayToMondayDelivery
            ),
            targetWeekStart = targetWeekStart,
            lumberRate = 0.0,
            steelRate = 0.0,
            isTrainingGlobal = false,
            trainerRate = 0.0
        )

        assertEquals(300.0, summary.weeklyPay, 0.001)
    }

    @Test
    fun excludesActiveLoadsAndUsesPickupDateForLegacyCompletedLoads() {
        val activeLoad = load(
            proNumber = "ACTIVE",
            pickupDate = LocalDate.of(2026, 10, 5),
            tripState = "ACTIVE",
            percentageRate = 100.0,
            loadPay = 100.0
        )
        val missingDelivery = load(
            proNumber = "NO-DELIVERY-DATE",
            pickupDate = LocalDate.of(2026, 10, 5),
            hasDeliveryTimestamp = false,
            percentageRate = 100.0,
            loadPay = 200.0
        )

        val summary = PayrollCalculator.calculateWeekSummary(
            loads = listOf(activeLoad, missingDelivery),
            targetWeekStart = targetWeekStart,
            lumberRate = 0.0,
            steelRate = 0.0,
            isTrainingGlobal = false,
            trainerRate = 0.0
        )

        assertEquals(200.0, summary.weeklyPay, 0.001)
    }

    private fun load(
        proNumber: String,
        pickupDate: LocalDate,
        deliveryDate: LocalDate? = pickupDate,
        hasDeliveryTimestamp: Boolean = true,
        tripState: String = "COMPLETED",
        dispatchedBounceMiles: Double = 0.0,
        dispatchedLoadedMiles: Double = 0.0,
        bounceMilesStart: Double = 0.0,
        bounceMilesEnd: Double = 0.0,
        loadedMilesStart: Double = 0.0,
        loadedMilesEnd: Double = 0.0,
        percentageRate: Double = 0.0,
        loadPay: Double = 0.0,
        tarpType: String = "N",
        isPreTarped: Boolean = false,
        isGoingHome: Boolean = false,
        isTrainingWeek: Boolean = false,
        trainerPayRate: Double = 0.0,
        extraStopCount: Int = 0
    ) = CurrentLoad(
        proNumber = proNumber,
        dispatchedBounceMiles = dispatchedBounceMiles,
        dispatchedLoadedMiles = dispatchedLoadedMiles,
        bounceMilesStart = bounceMilesStart,
        bounceMilesEnd = bounceMilesEnd,
        loadedMilesStart = loadedMilesStart,
        loadedMilesEnd = loadedMilesEnd,
        percentageRate = percentageRate,
        loadPay = loadPay,
        tarpType = tarpType,
        isPreTarped = isPreTarped,
        pickupTimestamp = pickupDate.atStartOfDay(payrollZone).toInstant().toEpochMilli(),
        isGoingHome = isGoingHome,
        tripState = tripState,
        deliveryTimestamp = if (hasDeliveryTimestamp) {
            deliveryDate?.atTime(12, 0)?.atZone(payrollZone)?.toInstant()?.toEpochMilli()
        } else {
            null
        },
        isTrainingWeek = isTrainingWeek,
        trainerPayRate = trainerPayRate,
        extraStopCount = extraStopCount
    )
}
