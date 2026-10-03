package com.loadtracker.pro

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class LoadViewModel(application: Application) :
    AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val loadDao = db.loadDao()

    private val _allLoads = MutableStateFlow<List<CurrentLoad>>(
        emptyList()
    )
    val allLoads: StateFlow<List<CurrentLoad>> =
        _allLoads.asStateFlow()

    val allFacilities: Flow<List<String>> = loadDao.getAllFacilityNames()
    
    private val _selectedFacility = MutableStateFlow("")
    val selectedFacility = _selectedFacility.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val facilityNotes: Flow<List<FacilityNote>> = _selectedFacility.flatMapLatest { name ->
        if (name.isBlank()) flowOf(emptyList())
        else loadDao.getNotesForFacility(name)
    }

    fun selectFacility(name: String) {
        _selectedFacility.value = name
    }

    fun getBreadcrumbs(pro: String): Flow<List<TripBreadcrumb>> {
        return loadDao.getBreadcrumbsForLoad(pro)
    }

    val allBreadcrumbs: Flow<List<TripBreadcrumb>> = loadDao.getAllBreadcrumbs()

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
        tripState: String,
        tripNotes: String? = null,
        deliveryTimestamp: Long? = null,
        shipperName: String? = null,
        shipperLat: Double? = null,
        shipperLong: Double? = null,
        consigneeName: String? = null,
        consigneeLat: Double? = null,
        consigneeLong: Double? = null,
        isTrainingWeek: Boolean = false,
        trainerPayRate: Double = 0.0,
        pickupApptText: String? = null,
        pickupApptTimestamp: Long? = null,
        pickupApptType: String? = null,
        consigneeApptText: String? = null,
        consigneeApptTimestamp: Long? = null,
        consigneeApptType: String? = null,
        dockArrivalTime: Long? = null,
        detentionHoursLogged: Double = 0.0,
        detentionFlatPay: Double = 0.0
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
                tripState = tripState,
                tripNotes = tripNotes,
                deliveryTimestamp = deliveryTimestamp,
                shipperName = shipperName,
                shipperLat = shipperLat,
                shipperLong = shipperLong,
                consigneeName = consigneeName,
                consigneeLat = consigneeLat,
                consigneeLong = consigneeLong,
                isTrainingWeek = isTrainingWeek,
                trainerPayRate = trainerPayRate,
                pickupApptText = pickupApptText,
                pickupApptTimestamp = pickupApptTimestamp,
                pickupApptType = pickupApptType,
                consigneeApptText = consigneeApptText,
                consigneeApptTimestamp = consigneeApptTimestamp,
                consigneeApptType = consigneeApptType,
                dockArrivalTime = dockArrivalTime,
                detentionHoursLogged = detentionHoursLogged,
                detentionFlatPay = detentionFlatPay
            )
            loadDao.insertLoad(newLoad)
        }
    }

    fun deleteLoad(load: CurrentLoad) {
        viewModelScope.launch {
            loadDao.deleteLoad(load)
        }
    }

    fun getCurrentWeekSummary(
        lumberRate: Double,
        steelRate: Double,
        isTrainingGlobal: Boolean,
        trainerRate: Double
    ): WeekSummary {
        val targetFriday = getPayPeriodDate(
            System.currentTimeMillis()
        )

        var totalPay = 0.0
        var totalActualMiles = 0.0
        var totalDispatchedMiles = 0.0
        var totalOutOfRouteMiles = 0.0
        var weekContainsTrainingLoad = false

        val weekLoads = _allLoads.value.filter { getPayPeriodDate(it.pickupTimestamp) == targetFriday }

        _allLoads.value.forEach { load ->
            val loadFriday = getPayPeriodDate(
                load.pickupTimestamp
            )

            if (loadFriday == targetFriday) {
                if (load.isTrainingWeek) weekContainsTrainingLoad = true
                
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

        // 📍 TRAINER LOGIC: Use stored snapshot trainerPayRate if present, else fallback to global trainerRate
        val weekTrainerPay = weekLoads.maxOfOrNull { it.trainerPayRate }?.takeIf { it > 0.0 }
            ?: if (weekContainsTrainingLoad || isTrainingGlobal) trainerRate else 0.0

        totalPay += weekTrainerPay

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

    fun generateCsvContent(): String {
        val sb = StringBuilder()
        // CSV Header
        sb.append("PRO Number,Pickup Date,Dispatched Bounce,Dispatched Loaded,Actual Bounce,Actual Loaded,Pay Split (%),Gross Pay,Tarp Type,Pre-Tarped,Going Home,Trip Notes,Net Pay\n")

        _allLoads.value.forEach { load ->
            val date = Instant.ofEpochMilli(load.pickupTimestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .toString()

            val actBounce = load.bounceMilesEnd - load.bounceMilesStart
            val actLoaded = load.loadedMilesEnd - load.loadedMilesStart

            // Calculate Net Pay for the CSV
            val baseSplit = load.loadPay * (load.percentageRate / 100.0)
            val bounceBonus = if (load.dispatchedBounceMiles >= 150.0) load.dispatchedBounceMiles * 0.20 else 0.0
            // Note: Tarp rates aren't stored in the load entity, so we'll just note the type
            val netPayEstimate = baseSplit + bounceBonus

            sb.append("${load.proNumber},")
            sb.append("$date,")
            sb.append("${String.format(Locale.US, "%.1f", load.dispatchedBounceMiles)},")
            sb.append("${String.format(Locale.US, "%.1f", load.dispatchedLoadedMiles)},")
            sb.append("${String.format(Locale.US, "%.1f", actBounce)},")
            sb.append("${String.format(Locale.US, "%.1f", actLoaded)},")
            sb.append("${String.format(Locale.US, "%.1f", load.percentageRate)},")
            sb.append("${String.format(Locale.US, "%.2f", load.loadPay)},")
            sb.append("${load.tarpType},")
            sb.append("${load.isPreTarped},")
            sb.append("${load.isGoingHome},")
            sb.append("\"${(load.tripNotes ?: "").replace("\"", "\"\"")}\",")
            sb.append("${String.format(Locale.US, "%.2f", netPayEstimate)}\n")
        }
        return sb.toString()
    }
}

data class WeekSummary(
    val weeklyPay: Double,
    val weeklyMilesDriven: Double,
    val weeklyOutOfRoute: Double,
    val weeklyOorPercentage: Double
)

fun getPayPeriodDate(timestamp: Long): LocalDate {
    val pDate = Instant.ofEpochMilli(timestamp)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()

    return pDate.with(TemporalAdjusters.next(DayOfWeek.FRIDAY))
}

