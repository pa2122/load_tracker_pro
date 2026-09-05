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
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Locale
import com.example.tmcloadtracker.R

class TrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var lastLocation: Location? = null

    companion object {
        const val CHANNEL_ID = "tracking_channel"
        const val NOTIFICATION_ID = 101

        // Static streams allowing your UI screens to display live odometer miles as you drive
        val totalBounceMilesTracked = MutableStateFlow(0.0)
        val totalLoadedMilesTracked = MutableStateFlow(0.0)
        var activeSegment = "Bounce" // "Bounce" or "Loaded"
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()

        // 1. CONFIGURE GPS LOCATION FREQUENCY REFRESH RULES
        val locationRequest =
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000) // Check every 5 seconds
                .setMinUpdateDistanceMeters(10f) // Only log if truck moved 10 meters (approx 32 feet)
                .build()

        // 2. DEFINE THE DISTANCE MATHEMATIC LOGIC CALLBACK ENGINE
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    if (lastLocation != null) {
                        // Calculate distance in meters between last point and current point
                        val distanceMeters = lastLocation!!.distanceTo(location)
                        // Convert meters to miles (1 meter = 0.000621371 miles)
                        val milesDriven = distanceMeters * 0.000621371

                        // Route the calculated mileage accumulation based on current trip status
                        if (activeSegment == "Bounce") {
                            totalBounceMilesTracked.value += milesDriven
                        } else if (activeSegment == "Loaded") {
                            totalLoadedMilesTracked.value += milesDriven
                        }
                        updateNotification()
                    }
                    lastLocation = location
                }
            }
        }

        // 3. START PULLING LIVE DATA STREAM
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (unlikely: SecurityException) {
            // Permissions missing
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateNotification()
        return START_STICKY // Forces background engine to automatically revive if crashed by system
    }

    private fun updateNotification() {
        val miles = if (activeSegment == "Bounce") totalBounceMilesTracked.value else totalLoadedMilesTracked.value
        val contentText = "$activeSegment: ${String.format(Locale.US, "%.1f", miles)} mi"

        // 4. LAUNCH PERSISTENT NOTIFICATION TO KEEP SERVICE ALIVE FOREVER IN BACKGROUND
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TMC Load Tracker - Active")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true) // Don't buzz the phone every 5 seconds
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
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(CHANNEL_ID, "GPS Tracking", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
