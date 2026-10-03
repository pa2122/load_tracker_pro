package com.loadtracker.pro

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
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

        val totalBounceMilesTracked = MutableStateFlow(0.0)
        val totalLoadedMilesTracked = MutableStateFlow(0.0)
        var activeSegment = "Bounce" // "Bounce", "Loaded", "Paused"
        
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

        fun saveState(context: Context) {
            val prefs = context.getSharedPreferences("tracking_service_prefs", MODE_PRIVATE)
            prefs.edit().apply {
                putString("activeProNumber", activeProNumber)
                putString("activeSegment", activeSegment)
                putFloat("homeLat", homeLat?.toFloat() ?: -999f)
                putFloat("homeLong", homeLong?.toFloat() ?: -999f)
                putFloat("targetLat", targetLat?.toFloat() ?: -999f)
                putFloat("targetLong", targetLong?.toFloat() ?: -999f)
                putString("targetName", targetName)
                putBoolean("isGeofenceActive", isGeofenceActive)
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
        }

        fun resetTrackingState() {
            totalBounceMilesTracked.value = 0.0
            totalLoadedMilesTracked.value = 0.0
            activeSegment = "Bounce"
            activeProNumber = null
            isGeofenceActive = false
            currentLatitude = null
            currentLongitude = null
        }
    }

    private var isLowPowerMode = false

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
        // 1. Check for Home Arrival if on a "Going Home" load
        if (homeLat != null && homeLong != null) {
            val homeLoc = Location("").apply {
                latitude = homeLat!!
                longitude = homeLong!!
            }
            if (currentLocation.distanceTo(homeLoc) < 305.0) { // 1,000 feet
                // 📍 AUTO-PAUSE LOGIC
                activeSegment = "PAUSED_AT_HOME"
                isGeofenceActive = false
                updateNotification()

                // Update Database so Dashboard reflects the "At Home" state
                val pro = activeProNumber
                if (pro != null) {
                    serviceScope.launch {
                        try {
                            val dao = AppDatabase.getDatabase(applicationContext).loadDao()
                            dao.updateTripState(pro, "PAUSED_AT_HOME")
                            // 📍 STOP SERVICE TO SAVE BATTERY & PRIVACY
                            stopSelf()
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }
                return 
            }
        }

        // 2. Standard Facility Geofencing (Pro Feature)
        if (!isProUser || !isGeofenceActive || targetLat == null || targetLong == null) return

        val targetLoc = Location("").apply {
            latitude = targetLat!!
            longitude = targetLong!!
        }

        val distanceToTarget = currentLocation.distanceTo(targetLoc)
        
        if (distanceToTarget < 305.0) { // 1,000 feet
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
                        if (previousSegment == "Bounce") {
                            dao.updateTripState(pro, "ACTIVE_SHIPPER")
                        } else if (previousSegment == "Loaded") {
                            dao.updateTripState(pro, "ACTIVE_CONSIGNEE")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
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
        updateNotification()
        configureLocationUpdates()
        return START_STICKY
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
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        lastLocation = null
        resetTrackingState()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel =
            NotificationChannel(CHANNEL_ID, "GPS Tracking", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }
}
