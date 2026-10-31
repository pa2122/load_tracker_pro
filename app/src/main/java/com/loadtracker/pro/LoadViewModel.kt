package com.loadtracker.pro

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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

    private val _loadsLoaded = MutableStateFlow(false)
    val loadsLoaded: StateFlow<Boolean> = _loadsLoaded.asStateFlow()

    val facilities: Flow<List<FacilityNoteEntity>> = loadDao.getAllFacilityNotes()

    private val _selectedFacility = MutableStateFlow("")
    val selectedFacility = _selectedFacility.asStateFlow()

    fun selectFacility(key: String) {
        _selectedFacility.value = key
    }

    suspend fun getFacilityNote(name: String): FacilityNoteEntity? =
        loadDao.getFacilityNote(FacilityNotes.key(name))

    suspend fun saveFacilityNote(note: FacilityNoteEntity) {
        DatabaseBackupManager.createDailyBackupBeforeMutation(getApplication())
        loadDao.upsertFacilityNote(note)
    }

    fun getBreadcrumbs(pro: String): Flow<List<TripBreadcrumb>> {
        return loadDao.getBreadcrumbsForLoad(pro)
    }

    val allBreadcrumbs: Flow<List<TripBreadcrumb>> = loadDao.getAllBreadcrumbs()

    init {
        viewModelScope.launch {
            loadDao.getAllLoads().collect { list ->
                _allLoads.value = list
                _loadsLoaded.value = true
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
        detentionFlatPay: Double = 0.0,
        extraStopCount: Int = 0,
        stopOffsData: String = "",
        currentStopIndex: Int = 0,
        lastStopLoadedMiles: Double = 0.0
    ) {
        viewModelScope.launch {
            DatabaseBackupManager.createDailyBackupBeforeMutation(getApplication())
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
                detentionFlatPay = detentionFlatPay,
                extraStopCount = extraStopCount,
                stopOffsData = stopOffsData,
                currentStopIndex = currentStopIndex,
                lastStopLoadedMiles = lastStopLoadedMiles
            )
            loadDao.insertLoad(newLoad)
        }
    }

    fun updateDeliveryTimestamp(load: CurrentLoad, deliveryTimestamp: Long) {
        viewModelScope.launch {
            DatabaseBackupManager.createDailyBackupBeforeMutation(getApplication())
            loadDao.insertLoad(load.copy(deliveryTimestamp = deliveryTimestamp))
        }
    }

    fun deleteLoad(load: CurrentLoad) {
        viewModelScope.launch {
            DatabaseBackupManager.createDailyBackupBeforeMutation(getApplication())
            loadDao.deleteLoad(load)
        }
    }

    fun updateTripState(proNumber: String, newState: String) {
        viewModelScope.launch {
            DatabaseBackupManager.createDailyBackupBeforeMutation(getApplication())
            loadDao.updateTripState(proNumber, newState)
        }
    }

    fun getCurrentWeekSummary(
        lumberRate: Double,
        steelRate: Double,
        extraStopRate: Double,
        isTrainingGlobal: Boolean,
        trainerRate: Double
    ): WeekSummary {
        return PayrollCalculator.calculateWeekSummary(
            loads = _allLoads.value,
            targetWeekStart = getPayrollWeekStartDate(System.currentTimeMillis()),
            lumberRate = lumberRate,
            steelRate = steelRate,
            extraStopRate = extraStopRate,
            isTrainingGlobal = isTrainingGlobal,
            trainerRate = trainerRate
        )
    }

    fun generateCsvContent(extraStopRate: Double = 0.0): String {
        val sb = StringBuilder()
        // CSV Header
        sb.append("PRO Number,Pickup Date,Dispatched Bounce,Dispatched Loaded,Actual Bounce,Actual Loaded,Pay Split (%),Gross Pay,Tarp Type,Pre-Tarped,Extra Stops,Extra Stop Pay,Going Home,Trip Notes,Net Pay\n")

        _allLoads.value.forEach { load ->
            val date = Instant.ofEpochMilli(load.pickupTimestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .toString()

            val actBounce = load.bounceMilesEnd - load.bounceMilesStart
            val actLoaded = load.loadedMilesEnd - load.loadedMilesStart

            // Calculate Net Pay for the CSV in integer cents
            val grossCents = load.loadPay.toCents()
            val baseSplitCents = Math.round(grossCents * (load.percentageRate / 100.0))
            val bounceBonusCents = (if (load.dispatchedBounceMiles >= 150.0) load.dispatchedBounceMiles * 0.20 else 0.0).toCents()
            val extraStopPayCents = load.extraStopPayCents(extraStopRate)
            val netPayEstimate = (baseSplitCents + bounceBonusCents + extraStopPayCents).toDollars()

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
            sb.append("${load.extraStopCount},")
            sb.append("${String.format(Locale.US, "%.2f", load.extraStopPayCents(extraStopRate).toDollars())},")
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

fun Double.toCents(): Long = Math.round(this * 100.0)
fun Long.toDollars(): Double = this / 100.0
