package com.loadtracker.pro

object TruckStopGeofenceEngine {

    data class TruckStopInfo(
        val name: String,
        val chain: String, // "Love's", "Pilot Flying J", "TA / Petro"
        val state: String,
        val latitude: Double,
        val longitude: Double,
        val radiusMeters: Double = 500.0 // ~1,600 feet property boundary
    )

    // Pre-loaded geofence coordinates for major interstate truck stops
    val MAJOR_TRUCK_STOPS = listOf(
        TruckStopInfo("Love's Travel Stop #410", "Love's", "TX", 28.0371, -97.5083),
        TruckStopInfo("Pilot Flying J #205", "Pilot Flying J", "AR", 33.6671, -93.5912),
        TruckStopInfo("TA TravelCenter #102", "TA / Petro", "LA", 32.4606, -93.7502),
        TruckStopInfo("Love's Travel Stop #610", "Love's", "MS", 32.3021, -90.1116),
        TruckStopInfo("Pilot Flying J #314", "Pilot Flying J", "TN", 35.0829, -89.9310),
        TruckStopInfo("Kwik Trip #804", "Kwik Trip", "WI", 43.8138, -91.2519),
        TruckStopInfo("Speedway #5210", "Speedway", "OH", 39.9612, -82.9988)
    )

    /**
     * Checks if current GPS coordinates are inside a major truck stop property boundary.
     */
    fun findNearbyTruckStop(latitude: Double, longitude: Double): TruckStopInfo? {
        return MAJOR_TRUCK_STOPS.firstOrNull { stop ->
            val distMeters = calculateDistanceMeters(latitude, longitude, stop.latitude, stop.longitude)
            distMeters <= stop.radiusMeters
        }
    }

    private fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}
