package com.loadtracker.pro

object IftaStateDetector {

    data class StateBoundingBox(
        val stateCode: String,
        val minLat: Double,
        val maxLat: Double,
        val minLong: Double,
        val maxLong: Double
    )

    // Bounding box definitions for key trucking states
    private val US_STATE_BOUNDS = listOf(
        StateBoundingBox("TX", 25.8371, 36.5007, -106.6456, -93.5083),
        StateBoundingBox("LA", 28.9286, 33.0195, -94.0431, -88.8170),
        StateBoundingBox("AR", 33.0041, 36.4997, -94.6179, -89.6448),
        StateBoundingBox("OK", 33.6158, 37.0023, -103.0026, -94.4307),
        StateBoundingBox("MS", 30.1739, 34.9961, -91.6550, -88.0979),
        StateBoundingBox("AL", 30.1739, 35.0080, -88.4732, -84.8882),
        StateBoundingBox("TN", 34.9829, 36.6781, -90.3103, -81.6469),
        StateBoundingBox("GA", 30.3558, 35.0007, -85.6052, -80.8397),
        StateBoundingBox("SC", 32.0346, 35.2154, -83.3539, -78.5411),
        StateBoundingBox("NC", 33.8423, 36.5881, -84.3219, -75.4606),
        StateBoundingBox("FL", 24.3963, 31.0009, -87.6349, -80.0314),
        StateBoundingBox("NM", 31.3323, 37.0002, -109.0502, -103.0020),
        StateBoundingBox("MO", 35.9957, 40.6136, -95.7747, -89.0988),
        StateBoundingBox("KS", 36.9930, 40.0032, -102.0517, -94.5884)
    )

    /**
     * Resolves a state code from GPS latitude and longitude using offline bounding boxes.
     */
    fun detectStateCode(latitude: Double, longitude: Double): String {
        val match = US_STATE_BOUNDS.firstOrNull { box ->
            latitude >= box.minLat && latitude <= box.maxLat &&
                    longitude >= box.minLong && longitude <= box.maxLong
        }
        return match?.stateCode ?: "US"
    }

    /**
     * Aggregates breadcrumb GPS points into state-by-state mileage totals.
     */
    fun calculateStateMileage(breadcrumbs: List<TripBreadcrumb>): Map<String, Double> {
        if (breadcrumbs.size < 2) return emptyMap()

        val resultMap = mutableMapOf<String, Double>()
        var previous = breadcrumbs.first()
        var currentState = detectStateCode(previous.latitude, previous.longitude)

        for (i in 1 until breadcrumbs.size) {
            val current = breadcrumbs[i]
            val nextState = detectStateCode(current.latitude, current.longitude)

            // Approximate distance between points in miles
            val distMiles = calculateApproxMiles(
                previous.latitude, previous.longitude,
                current.latitude, current.longitude
            )

            val stateKey = if (nextState != "US") nextState else currentState
            resultMap[stateKey] = (resultMap[stateKey] ?: 0.0) + distMiles

            previous = current
            currentState = nextState
        }

        return resultMap
    }

    private fun calculateApproxMiles(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 3958.8 // Earth radius in miles
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}
