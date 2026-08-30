package com.example.tmcloadtracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

class LoadViewModel(application: Application) :
    AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val loadDao = db.loadDao()

    private val _allLoads = MutableStateFlow<List<CurrentLoad>>(
        emptyList()
    )
    val allLoads: StateFlow<List<CurrentLoad>> =
        _allLoads.asStateFlow()

    init {
        viewModelScope.launch {
            loadDao.getAllLoads().collect { list ->
                _allLoads.value = list
            }
        }
    }

    fun saveLoad(
        proNumber: String,
        dispBounce: Double,
        dispLoaded: Double,
        bounceStart: Double,
        bounceEnd: Double,
        loadedStart: Double,
        loadedEnd: Double,
        ratePercent: Double,
        loadPay: Double,
        tarpType: String,
        isPreTarped: Boolean,
        pickupTimestamp: Long,
        isGoingHome: Boolean,
        tripState: String
    ) {
        viewModelScope.launch {
            val newLoad = CurrentLoad(
                proNumber = proNumber,
                dispatchedBounceMiles = dispBounce,
                dispatchedLoadedMiles = dispLoaded,
                bounceMilesStart = bounceStart,
                bounceMilesEnd = bounceEnd,
                loadedMilesStart = loadedStart,
                loadedMilesEnd = loadedEnd,
                percentageRate = ratePercent,
                loadPay = loadPay,
                tarpType = tarpType,
                isPreTarped = isPreTarped,
                pickupTimestamp = pickupTimestamp,
                isGoingHome = isGoingHome,
                tripState = tripState
            )
            loadDao.insertLoad(newLoad)
        }
    }

    fun getCurrentWeekSummary(
        lumberRate: Double,
        steelRate: Double,
        isTraining: Boolean,
        trainerRate: Double
    ): WeekSummary {
        val targetFriday = getPayPeriodDate(
            System.currentTimeMillis()
        )

        var totalPay = 0.0
        var totalActualMiles = 0.0
        var totalDispatchedMiles = 0.0
        var totalOutOfRouteMiles = 0.0

        _allLoads.value.forEach { load ->
            val loadFriday = getPayPeriodDate(
                load.pickupTimestamp
            )

            if (loadFriday == targetFriday) {
                val baseSplit = load.loadPay * (
                        load.percentageRate / 100.0
                        )

                var tarpPay = when (load.tarpType) {
                    "L" -> lumberRate
                    "S" -> steelRate
                    else -> 0.0
                }
                if (load.isPreTarped) {
                    tarpPay /= 2.0
                }

                val bounceBonus = if (
                    load.dispatchedBounceMiles >= 150.0
                ) {
                    load.dispatchedBounceMiles * 0.20
                } else {
                    0.0
                }

                totalPay += (baseSplit + tarpPay + bounceBonus)

                val actBounce = load.bounceMilesEnd -
                        load.bounceMilesStart
                val actLoaded = load.loadedMilesEnd -
                        load.loadedMilesStart
                val actTrip = actBounce + actLoaded
                val dispTrip = load.dispatchedBounceMiles +
                        load.dispatchedLoadedMiles

                totalActualMiles += actTrip
                totalDispatchedMiles += dispTrip

                if (load.isGoingHome) {
                    totalOutOfRouteMiles += 0.0
                } else {
                    val tripOor = actTrip - dispTrip
                    if (tripOor > 0.0) {
                        totalOutOfRouteMiles += tripOor
                    }
                }
            }
        }

        // 📍 MATH FIX: Trainer bonus gets added to totalPay pool BEFORE returning data
        if (isTraining) {
            totalPay += trainerRate
        }

        val oorPct = if (totalDispatchedMiles > 0.0) {
            (totalOutOfRouteMiles / totalDispatchedMiles) * 100.0
        } else {
            0.0
        }

        return WeekSummary(
            weeklyPay = totalPay,
            weeklyMilesDriven = totalActualMiles,
            weeklyOutOfRoute = totalOutOfRouteMiles,
            weeklyOorPercentage = oorPct
        )
    }

    private fun getPayPeriodDate(timestamp: Long): LocalDate {
        val pDate = Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        return when (pDate.dayOfWeek) {
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY -> {
                pDate.with(TemporalAdjusters.next(DayOfWeek.FRIDAY))
            }

            else -> {
                pDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY))
            }
        }
    }
}

data class WeekSummary(
    val weeklyPay: Double,
    val weeklyMilesDriven: Double,
    val weeklyOutOfRoute: Double,
    val weeklyOorPercentage: Double
)
