package com.loadtracker.pro

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class TrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var lastLocation: Location? = null
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastBreadcrumbTime = 0L

    companion object {
        const val CHANNEL_ID = "tracking_channel"
        const val NOTIFICATION_ID = 101
        private const val ACTION_NOTIFICATION_TRIP_STEP = "com.loadtracker.pro.NOTIFICATION_TRIP_STEP"
        private const val EXTRA_NOTIFICATION_PRO = "notification_pro"
        private const val EXTRA_EXPECTED_TRIP_STATE = "expected_trip_state"

        val totalBounceMilesTracked = MutableStateFlow(0.0)
        val totalLoadedMilesTracked = MutableStateFlow(0.0)
        var activeSegment = "Bounce" // "Bounce", "Loaded", "Paused"
        var activeTripState: String? = null
        
        var activeProNumber: String? = null

        // 📍 NEW: Pinned Home for Auto-Pause
        var homeLat: Double? = null
        var homeLong: Double? = null

        var targetLat: Double? = null
        var targetLong: Double? = null
        var targetName: String? = null
        var isGeofenceActive = false
        var isProUser = true

        var currentLatitude: Double? = null
        var currentLongitude: Double? = null
        var detectedTruckStop: String? = null
        var departureGraceTimeMs: Long = 0L

        const val ARRIVAL_RADIUS_METERS = 305.0
        const val ARRIVAL_MAX_SPEED_MPS = 2.24
        const val ARRIVAL_DWELL_MS = 30_000L

        @JvmStatic
        fun shouldTriggerArrival(
            currentLocation: Location,
            targetLocation: Location,
            previousLocation: Location? = null,
            candidateStartMs: Long? = null,
            nowMs: Long = System.currentTimeMillis()
        ): Pair<Boolean, Long?> {
            val distanceMeters = currentLocation.distanceTo(targetLocation).toDouble()
            val speedMps = when {
                currentLocation.hasSpeed() -> currentLocation.speed.toDouble()
                previousLocation != null && previousLocation.time > 0L && currentLocation.time > previousLocation.time -> {
                    currentLocation.distanceTo(previousLocation).toDouble() / ((currentLocation.time - previousLocation.time) / 1000.0)
                }
                else -> Double.POSITIVE_INFINITY
            }
            return shouldTriggerArrival(
                distanceMeters = distanceMeters,
                speedMps = speedMps,
                candidateStartMs = candidateStartMs,
                nowMs = nowMs,
            )
        }

        @JvmStatic
        fun shouldTriggerArrival(
            distanceMeters: Double,
            speedMps: Double,
            candidateStartMs: Long? = null,
            nowMs: Long = System.currentTimeMillis()
        ): Pair<Boolean, Long?> {
            if (distanceMeters >= ARRIVAL_RADIUS_METERS) {
                return false to null
            }

            if (speedMps > ARRIVAL_MAX_SPEED_MPS) {
                return false to null
            }

            val startMs = candidateStartMs ?: nowMs
            val triggered = (nowMs - startMs) >= ARRIVAL_DWELL_MS
            return triggered to if (triggered) null else startMs
        }

        fun saveState(context: Context) {
            val prefs = context.getSharedPreferences("tracking_service_prefs", MODE_PRIVATE)
            prefs.edit().apply {
                putString("activeProNumber", activeProNumber)
                putString("activeSegment", activeSegment)
                putString("activeTripState", activeTripState)
                putFloat("homeLat", homeLat?.toFloat() ?: -999f)
                putFloat("homeLong", homeLong?.toFloat() ?: -999f)
                putFloat("targetLat", targetLat?.toFloat() ?: -999f)
                putFloat("targetLong", targetLong?.toFloat() ?: -999f)
                putString("targetName", targetName)
                putBoolean("isGeofenceActive", isGeofenceActive)
                putFloat("totalBounceMilesTracked", totalBounceMilesTracked.value.toFloat())
                putFloat("totalLoadedMilesTracked", totalLoadedMilesTracked.value.toFloat())
                apply()
            }
        }

        fun restoreState(context: Context) {
            val prefs = context.getSharedPreferences("tracking_service_prefs", MODE_PRIVATE)
            if (activeProNumber == null) {
                activeProNumber = prefs.getString("activeProNumber", null)
            }
            val savedSegment = prefs.getString("activeSegment", null)
            if (savedSegment != null) activeSegment = savedSegment
            if (activeTripState == null) {
                activeTripState = prefs.getString("activeTripState", null)
            }

            val hLat = prefs.getFloat("homeLat", -999f)
            if (hLat != -999f) homeLat = hLat.toDouble()
            val hLong = prefs.getFloat("homeLong", -999f)
            if (hLong != -999f) homeLong = hLong.toDouble()

            val tLat = prefs.getFloat("targetLat", -999f)
            if (tLat != -999f) targetLat = tLat.toDouble()
            val tLong = prefs.getFloat("targetLong", -999f)
            if (tLong != -999f) targetLong = tLong.toDouble()

            val tName = prefs.getString("targetName", null)
            if (tName != null) targetName = tName

            isGeofenceActive = prefs.getBoolean("isGeofenceActive", isGeofenceActive)

            val savedBounce = prefs.getFloat("totalBounceMilesTracked", 0f)
            if (totalBounceMilesTracked.value == 0.0 && savedBounce > 0f) {
                totalBounceMilesTracked.value = savedBounce.toDouble()
            }
            val savedLoaded = prefs.getFloat("totalLoadedMilesTracked", 0f)
            if (totalLoadedMilesTracked.value == 0.0 && savedLoaded > 0f) {
                totalLoadedMilesTracked.value = savedLoaded.toDouble()
            }
        }

        fun resetTrackingState(context: Context? = null) {
            totalBounceMilesTracked.value = 0.0
            totalLoadedMilesTracked.value = 0.0
            activeSegment = "Bounce"
            activeTripState = null
            activeProNumber = null
            isGeofenceActive = false
            currentLatitude = null
            currentLongitude = null
            context?.getSharedPreferences("tracking_service_prefs", MODE_PRIVATE)?.edit()?.clear()?.apply()
        }
    }

    private var isLowPowerMode = false
    private var geofenceCandidateStartMs: Long? = null
    private var homeCandidateStartMs: Long? = null

    fun configureLocationUpdates(force: Boolean = false) {
        val shouldBeLowPower = (activeSegment == "Paused" || activeSegment == "PAUSED_AT_HOME")
        if (!force && shouldBeLowPower == isLowPowerMode) return
        isLowPowerMode = shouldBeLowPower

        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            val priority = if (isLowPowerMode) Priority.PRIORITY_BALANCED_POWER_ACCURACY else Priority.PRIORITY_HIGH_ACCURACY
            val interval = if (isLowPowerMode) 60000L else 5000L
            val minDistance = if (isLowPowerMode) 25f else 10f

            val locationRequest = LocationRequest.Builder(priority, interval)
                .setMinUpdateDistanceMeters(minDistance)
                .build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    override fun onCreate() {
        super.onCreate()
        restoreState(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        updateNotification()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    if (!GpsLocationFilter.isValidLocation(location, lastLocation)) {
                        continue
                    }
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude

                    if (lastLocation != null) {
                        val distanceMeters = lastLocation!!.distanceTo(location)
                        val milesDriven = distanceMeters * 0.000621371

                        if (activeSegment == "Bounce") {
                            totalBounceMilesTracked.value += milesDriven
                        } else if (activeSegment == "Loaded") {
                            totalLoadedMilesTracked.value += milesDriven
                        }
                        updateNotification()
                        checkGeofence(location)

                        // 📍 Record Breadcrumb every 2.5 minutes for smooth route map geometry
                        val now = System.currentTimeMillis()
                        if ((activeProNumber != null) && (activeSegment != "Paused") && (now - lastBreadcrumbTime > 150000)) {
                            saveBreadcrumb(location)
                            lastBreadcrumbTime = now
                        }
                    }
                    lastLocation = location
                }
            }
        }

        configureLocationUpdates(force = true)
    }

    private fun saveBreadcrumb(loc: Location) {
        val pro = activeProNumber ?: return
        serviceScope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                db.loadDao().insertBreadcrumb(
                    TripBreadcrumb(
                        proNumber = pro,
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        timestamp = System.currentTimeMillis(),
                    ),
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun checkGeofence(currentLocation: Location) {
        val nowMs = System.currentTimeMillis()

        // 1. Check for Home Arrival if on a "Going Home" load
        if (homeLat != null && homeLong != null) {
            val homeLoc = Location("").apply {
                latitude = homeLat!!
                longitude = homeLong!!
            }
            val (shouldPauseHome, nextHomeCandidateStart) = shouldTriggerArrival(
                currentLocation = currentLocation,
                targetLocation = homeLoc,
                previousLocation = lastLocation,
                candidateStartMs = homeCandidateStartMs,
                nowMs = nowMs,
            )
            homeCandidateStartMs = nextHomeCandidateStart
            if (shouldPauseHome) {
                homeCandidateStartMs = null
                activeSegment = "PAUSED_AT_HOME"
                activeTripState = "PAUSED_AT_HOME"
                isGeofenceActive = false
                updateNotification()

                val pro = activeProNumber
                if (pro != null) {
                    serviceScope.launch {
                        try {
                            val dao = AppDatabase.getDatabase(applicationContext).loadDao()
                            dao.updateTripState(pro, "PAUSED_AT_HOME")
                            stopSelf()
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }
                return
            }
        }

        // 2. Standard Facility Geofencing (Pro Feature)
        if (!isProUser || !isGeofenceActive || targetLat == null || targetLong == null) return

        if (departureGraceTimeMs > 0L && nowMs - departureGraceTimeMs < 300000L) {
            return
        }

        val targetLoc = Location("").apply {
            latitude = targetLat!!
            longitude = targetLong!!
        }

        val (shouldTriggerArrival, nextCandidateStart) = shouldTriggerArrival(
            currentLocation = currentLocation,
            targetLocation = targetLoc,
            previousLocation = lastLocation,
            candidateStartMs = geofenceCandidateStartMs,
            nowMs = nowMs,
        )
        geofenceCandidateStartMs = nextCandidateStart
        if (!shouldTriggerArrival) return
        geofenceCandidateStartMs = null

        val previousSegment = activeSegment
        activeSegment = "Paused"
        isGeofenceActive = false
        updateNotification()
        configureLocationUpdates()

        val pro = activeProNumber
        if (pro != null) {
            serviceScope.launch {
                try {
                    val dao = AppDatabase.getDatabase(applicationContext).loadDao()
                    val nextState = when (previousSegment) {
                        "Bounce" -> "ACTIVE_SHIPPER"
                        "Loaded" -> "ACTIVE_CONSIGNEE"
                        else -> null
                    }
                    if (nextState != null) {
                        dao.updateTripState(pro, nextState)
                        activeTripState = nextState
                        saveState(applicationContext)
                        withContext(Dispatchers.Main) { updateNotification() }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("TrackingService", "Unable to save destination arrival", e)
                }
            }
        }
        // 3. Truck Stop Fueling Prompt Geofence
        val nearbyTruckStop = TruckStopGeofenceEngine.findNearbyTruckStop(currentLocation.latitude, currentLocation.longitude)
        if (nearbyTruckStop != null && activeSegment != "Paused" && activeSegment != "PAUSED_AT_HOME") {
            detectedTruckStop = nearbyTruckStop.name
            updateNotification()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_NOTIFICATION_TRIP_STEP) {
            handleNotificationTripStep(intent)
        } else {
            updateNotification()
            configureLocationUpdates()
        }
        return START_STICKY
    }

    private fun handleNotificationTripStep(intent: Intent) {
        val pro = intent.getStringExtra(EXTRA_NOTIFICATION_PRO) ?: return
        val expectedState = intent.getStringExtra(EXTRA_EXPECTED_TRIP_STATE) ?: return
        if (pro != activeProNumber) return

        serviceScope.launch {
            try {
                val dao = AppDatabase.getDatabase(applicationContext).loadDao()
                val load = dao.getLoadByProNumber(pro) ?: return@launch
                if (load.tripState != expectedState) {
                    activeTripState = load.tripState
                    withContext(Dispatchers.Main) { updateNotification() }
                    return@launch
                }

                val now = System.currentTimeMillis()
                val newState = when (expectedState) {
                    "ACTIVE_BOUNCE" -> "ACTIVE_SHIPPER"
                    "ACTIVE_SHIPPER" -> "ACTIVE_LOADED"
                    "ACTIVE_LOADED" -> "ACTIVE_CONSIGNEE"
                    else -> return@launch
                }
                val changedRows = dao.updateTripFromNotification(
                    pro = pro,
                    expectedState = expectedState,
                    newState = newState,
                    bounceMilesEnd = totalBounceMilesTracked.value,
                    loadedMilesEnd = totalLoadedMilesTracked.value,
                    dockArrivalTime = if (newState == "ACTIVE_SHIPPER" || newState == "ACTIVE_CONSIGNEE") now else null
                )
                if (changedRows == 0) {
                    activeTripState = dao.getLoadByProNumber(pro)?.tripState
                    withContext(Dispatchers.Main) { updateNotification() }
                    return@launch
                }

                when (newState) {
                    "ACTIVE_SHIPPER" -> {
                        activeSegment = "Paused"
                        isGeofenceActive = false
                    }
                    "ACTIVE_LOADED" -> {
                        activeSegment = "Loaded"
                        targetLat = load.consigneeLat
                        targetLong = load.consigneeLong
                        targetName = load.consigneeName ?: "Consignee"
                        isGeofenceActive = load.consigneeLat != null && load.consigneeLong != null
                        departureGraceTimeMs = now
                    }
                    "ACTIVE_CONSIGNEE" -> {
                        activeSegment = "Paused"
                        isGeofenceActive = false
                    }
                }
                activeTripState = newState
                saveState(applicationContext)
                withContext(Dispatchers.Main) {
                    configureLocationUpdates()
                    updateNotification()
                }
            } catch (e: Exception) {
                android.util.Log.e("TrackingService", "Unable to apply notification trip action", e)
            }
        }
    }

    private fun updateNotification() {
        saveState(this)
        val miles = if (activeSegment == "Bounce") totalBounceMilesTracked.value else totalLoadedMilesTracked.value
        
        val hudTitle = when (activeSegment) {
            "Paused" -> "📍 ARRIVED AT ${targetName?.uppercase(Locale.US) ?: "DESTINATION"}"
            "PAUSED_AT_HOME" -> "🏠 HOME BASE DETECTED"
            else -> "Load Tracker Pro - Active"
        }

        val contentText = when (activeSegment) {
            "Paused" -> "Status: Arrived / Loading Mode"
            "PAUSED_AT_HOME" -> "GPS Paused - Enjoy your time off!"
            else -> "$activeSegment: ${String.format(Locale.US, "%.1f", miles)} mi"
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(hudTitle)
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .apply {
                val pro = activeProNumber
                val expectedState = activeTripState
                val action = expectedState?.let(TrackingNotificationAction::forTripState)
                if (pro != null && expectedState != null && action != null) {
                    val actionIntent = if (action == TrackingNotificationAction.OPEN_TO_COMPLETE) {
                        PendingIntent.getActivity(
                            this@TrackingService,
                            pro.hashCode(),
                            Intent(this@TrackingService, MainActivity::class.java),
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        )
                    } else {
                        val serviceIntent = Intent(this@TrackingService, TrackingService::class.java).apply {
                            this.action = ACTION_NOTIFICATION_TRIP_STEP
                            data = Uri.parse("loadtracker://notification/${Uri.encode(pro)}/$expectedState")
                            putExtra(EXTRA_NOTIFICATION_PRO, pro)
                            putExtra(EXTRA_EXPECTED_TRIP_STATE, expectedState)
                        }
                        PendingIntent.getService(
                            this@TrackingService,
                            pro.hashCode() xor expectedState.hashCode(),
                            serviceIntent,
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        )
                    }
                    addAction(
                        NotificationCompat.Action.Builder(
                            R.mipmap.ic_launcher,
                            getString(action.titleResource),
                            actionIntent
                        ).build()
                    )
                }
            }
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        saveState(this)
        fusedLocationClient.removeLocationUpdates(locationCallback)
        lastLocation = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel =
            NotificationChannel(CHANNEL_ID, "GPS Tracking", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }
}
