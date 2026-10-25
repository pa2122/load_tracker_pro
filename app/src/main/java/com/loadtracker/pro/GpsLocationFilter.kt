package com.loadtracker.pro

import android.location.Location
import android.util.Log

object GpsLocationFilter {
    private const val MAX_ACCURACY_METERS = 50f
    private const val MAX_STALE_TIME_MS = 60_000L
    private const val MAX_FUTURE_SKEW_MS = 10_000L
    private const val MAX_REALISTIC_SPEED_MPS = 45.0

    fun isValidFix(
        accuracy: Float,
        locationTime: Long,
        currentTime: Long,
        distanceMeters: Float?,
        timeDeltaSeconds: Double?
    ): Boolean {
        if (!accuracy.isFinite() || accuracy < 0f || accuracy > MAX_ACCURACY_METERS) {
            return false
        }
        if (locationTime <= 0L || currentTime <= 0L) {
            return false
        }
        if (locationTime < currentTime - MAX_STALE_TIME_MS) {
            return false
        }
        if (locationTime > currentTime && locationTime - currentTime > MAX_FUTURE_SKEW_MS) {
            return false
        }
        if ((distanceMeters == null) != (timeDeltaSeconds == null)) {
            return false
        }
        if (distanceMeters != null && timeDeltaSeconds != null) {
            if (!distanceMeters.isFinite() || distanceMeters < 0f ||
                !timeDeltaSeconds.isFinite() || timeDeltaSeconds <= 0.0
            ) {
                return false
            }
            if (distanceMeters / timeDeltaSeconds > MAX_REALISTIC_SPEED_MPS) {
                return false
            }
        }
        return true
    }

    fun isValidLocation(
        currentLat: Double,
        currentLong: Double,
        currentAccuracy: Float,
        currentTimeMs: Long,
        lastLat: Double?,
        lastLong: Double?,
        lastTimeMs: Long?,
        nowTimeMs: Long = System.currentTimeMillis()
    ): Boolean {
        if (!currentLat.isFinite() || !currentLong.isFinite() ||
            currentLat !in -90.0..90.0 || currentLong !in -180.0..180.0
        ) {
            return false
        }

        val hasPreviousCoordinates = lastLat != null || lastLong != null || lastTimeMs != null
        if (!hasPreviousCoordinates) {
            return isValidFix(currentAccuracy, currentTimeMs, nowTimeMs, null, null)
        }

        val previousLat = lastLat ?: return false
        val previousLong = lastLong ?: return false
        val previousTimeMs = lastTimeMs ?: return false
        if (!previousLat.isFinite() || !previousLong.isFinite() ||
            previousLat !in -90.0..90.0 || previousLong !in -180.0..180.0 ||
            previousTimeMs <= 0L || currentTimeMs <= previousTimeMs
        ) {
            return false
        }

        val timeDeltaSeconds = (currentTimeMs - previousTimeMs) / 1000.0
        val distanceMeters = calculateApproxDistance(previousLat, previousLong, currentLat, currentLong)
        return isValidFix(
            accuracy = currentAccuracy,
            locationTime = currentTimeMs,
            currentTime = nowTimeMs,
            distanceMeters = distanceMeters.toFloat(),
            timeDeltaSeconds = timeDeltaSeconds
        )
    }

    fun isValidLocation(location: Location?, lastLocation: Location? = null): Boolean {
        return try {
            val current = location ?: return false
            if (!current.hasAccuracy()) return false
            isValidLocation(
                currentLat = current.latitude,
                currentLong = current.longitude,
                currentAccuracy = current.accuracy,
                currentTimeMs = current.time,
                lastLat = lastLocation?.latitude,
                lastLong = lastLocation?.longitude,
                lastTimeMs = lastLocation?.time
            )
        } catch (e: RuntimeException) {
            Log.w("GpsLocationFilter", "Rejecting location because validation failed", e)
            false
        }
    }

    private fun calculateApproxDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusMeters = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = (
            Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
            ).coerceIn(0.0, 1.0)
        return earthRadiusMeters * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }
}
