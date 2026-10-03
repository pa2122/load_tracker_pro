package com.example.tmcloadtracker

import android.location.Location

object GpsLocationFilter {

    fun isValidLocation(
        currentLat: Double,
        currentLong: Double,
        currentAccuracy: Float,
        currentTimeMs: Long,
        lastLat: Double?,
        lastLong: Double?,
        lastTimeMs: Long?
    ): Boolean {
        // 1. Accuracy Check: Reject inaccurate fixes (> 50 meters radius)
        if (currentAccuracy > 50f) {
            return false
        }

        if (lastLat == null || lastLong == null || lastTimeMs == null) {
            return true
        }

        // 2. Timestamp Check: Reject stale or out-of-order locations
        val timeDeltaMs = currentTimeMs - lastTimeMs
        if (timeDeltaMs <= 0) {
            return false
        }

        // 3. Teleportation / Speed Check: Reject impossible speeds (> 120 mph / ~53.6 m/s)
        val distanceMeters = calculateApproxDistance(lastLat, lastLong, currentLat, currentLong)
        val speedMps = distanceMeters / (timeDeltaMs / 1000.0)

        // 53.64 m/s is approx 120 mph
        if (speedMps > 53.64) {
            return false
        }

        return true
    }

    private fun calculateApproxDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    fun isValidLocation(current: Location, last: Location?): Boolean {
        val currentAcc = if (current.hasAccuracy()) current.accuracy else 0f
        return isValidLocation(
            currentLat = current.latitude,
            currentLong = current.longitude,
            currentAccuracy = currentAcc,
            currentTimeMs = current.time,
            lastLat = last?.latitude,
            lastLong = last?.longitude,
            lastTimeMs = last?.time
        )
    }
}
