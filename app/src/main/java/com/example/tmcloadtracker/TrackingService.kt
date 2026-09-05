package com.example.tmcloadtracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
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

        var targetLat: Double? = null
        var targetLong: Double? = null
        var targetName: String? = null
        var isGeofenceActive = false

        var currentLatitude: Double? = null
        var currentLongitude: Double? = null
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()

        val locationRequest =
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateDistanceMeters(10f)
                .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
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

                        // 📍 NEW: Record Breadcrumb every 5 minutes OR every 5 miles
                        val now = System.currentTimeMillis()
                        if ((activeProNumber != null) && (activeSegment != "Paused") && (now - lastBreadcrumbTime > 300000)) {
                            saveBreadcrumb(location)
                            lastBreadcrumbTime = now
                        }
                    }
                    lastLocation = location
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper(),
            )
        } catch (_: SecurityException) {}
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
        if (!isGeofenceActive || targetLat == null || targetLong == null) return

        val targetLoc = Location("").apply {
            latitude = targetLat!!
            longitude = targetLong!!
        }

        val distanceToTarget = currentLocation.distanceTo(targetLoc)
        
        if (distanceToTarget < 305.0) { // 1,000 feet
            activeSegment = "Paused"
            isGeofenceActive = false 
            updateNotification()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateNotification()
        return START_STICKY
    }

    private fun updateNotification() {
        val miles = if (activeSegment == "Bounce") totalBounceMilesTracked.value else totalLoadedMilesTracked.value
        
        val hudTitle = if (activeSegment == "Paused") {
            "📍 ARRIVED AT ${targetName?.uppercase(Locale.US) ?: "DESTINATION"}"
        } else {
            "Load Tracker Pro - Active"
        }
        val contentText = if (activeSegment == "Paused") "Status: Arrived / Loading Mode" else "$activeSegment: ${String.format(Locale.US, "%.1f", miles)} mi"

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
        isGeofenceActive = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel =
            NotificationChannel(CHANNEL_ID, "GPS Tracking", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }
}
