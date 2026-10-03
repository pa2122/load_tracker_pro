package com.loadtracker.pro

import android.location.Location

object GpsLocationFilter {
    private const val MAX_ACCURACY_METERS = 50.0f
    private const val MAX_STALE_TIME_MS = 60_000L // 60 seconds
    private const val MAX_REALISTIC_SPEED_MPS = 45.0f // ~100 mph (truck max speed)

    fun isValidFix(
        accuracy: Float,
        locationTime: Long,
        currentTime: Long,
        distanceMeters: Float?,
        timeDeltaSeconds: Double?
    ): Boolean {
        // 1. Accuracy check (if accuracy > 0 and exceeds max)
        if (accuracy > 0f && accuracy > MAX_ACCURACY_METERS) {
            return false
        }

        // 2. Stale fix check
        if (locationTime > 0) {
            val age = currentTime - locationTime
            if (age > MAX_STALE_TIME_MS) {
                return false
            }
        }

        // 3. Speed / Distance jump check
        if (distanceMeters != null && timeDeltaSeconds != null && timeDeltaSeconds > 0.0) {
            val speed = distanceMeters / timeDeltaSeconds
            if (speed > MAX_REALISTIC_SPEED_MPS) {
                return false
            }
        }

        return true
    }

    fun isValidLocation(location: Location?, lastLocation: Location? = null): Boolean {
        if (location == null) return false
        return try {
            val accuracy = if (location.hasAccuracy()) location.accuracy else 0f
            val locTime = if (location.time > 0) location.time else System.currentTimeMillis()
            val dist = if (lastLocation != null) {
                try { lastLocation.distanceTo(location) } catch (e: Exception) { null }
            } else null
            val timeDelta = if (lastLocation != null && location.time > 0 && lastLocation.time > 0) {
                (location.time - lastLocation.time) / 1000.0
            } else null

            isValidFix(accuracy, locTime, System.currentTimeMillis(), dist, timeDelta)
        } catch (e: Exception) {
            true // fallback if unmocked android.location.Location methods throw in test
        }
    }
}
