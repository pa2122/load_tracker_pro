package com.loadtracker.pro

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
        if (!currentLat.isFinite() || !currentLong.isFinite() || currentAccuracy.isNaN() || currentAccuracy.isInfinite()) {
            return false
        }

        if (currentLat < -90.0 || currentLat > 90.0 || currentLong < -180.0 || currentLong > 180.0) {
            return false
        }

        if (currentAccuracy < 0f || currentAccuracy > 50f) {
            return false
        }

        if (lastLat == null || lastLong == null || lastTimeMs == null) {
            return true
        }

        if (!lastLat.isFinite() || !lastLong.isFinite() || lastTimeMs < 0L) {
            return false
        }

        if (currentTimeMs <= lastTimeMs) {
            return false
        }

        val timeDeltaMs = currentTimeMs - lastTimeMs
        val distanceMeters = calculateApproxDistance(lastLat, lastLong, currentLat, currentLong)
        val speedMps = distanceMeters / (timeDeltaMs / 1000.0)

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
