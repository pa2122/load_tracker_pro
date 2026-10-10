package com.loadtracker.pro

object IftaStateDetector {

    data class StateBoundingBox(
        val stateCode: String,
        val minLat: Double,
        val maxLat: Double,
        val minLong: Double,
        val maxLong: Double
    )

    // Complete Lower-48 US States bounding box definitions for IFTA state line geofencing
    private val US_STATE_BOUNDS = listOf(
        StateBoundingBox("AL", 30.1739, 35.0080, -88.4732, -84.8882),
        StateBoundingBox("AZ", 31.3322, 37.0043, -114.8165, -109.0452),
        StateBoundingBox("AR", 33.0041, 36.4997, -94.6179, -89.6448),
        StateBoundingBox("CA", 32.5342, 42.0095, -124.4096, -114.1312),
        StateBoundingBox("CO", 36.9924, 41.0034, -109.0603, -102.0415),
        StateBoundingBox("CT", 40.9801, 42.0506, -73.7278, -71.7870),
        StateBoundingBox("DE", 38.4510, 39.8390, -75.7886, -75.0489),
        StateBoundingBox("FL", 24.3963, 31.0009, -87.6349, -80.0314),
        StateBoundingBox("GA", 30.3558, 35.0007, -85.6052, -80.8397),
        StateBoundingBox("ID", 41.9880, 49.0011, -117.2430, -111.0435),
        StateBoundingBox("IL", 36.9703, 42.5085, -91.5131, -87.4947),
        StateBoundingBox("IN", 37.7717, 41.7607, -88.0978, -84.7846),
        StateBoundingBox("IA", 40.3756, 43.5012, -96.6397, -90.1401),
        StateBoundingBox("KS", 36.9930, 40.0032, -102.0517, -94.5884),
        StateBoundingBox("KY", 36.4971, 39.1475, -89.5715, -81.9649),
        StateBoundingBox("LA", 28.9286, 33.0195, -94.0431, -88.8170),
        StateBoundingBox("ME", 43.0581, 47.4597, -71.0839, -66.9499),
        StateBoundingBox("MD", 37.8864, 39.7230, -79.4877, -75.0489),
        StateBoundingBox("MA", 41.2380, 42.8868, -73.5081, -69.9284),
        StateBoundingBox("MI", 41.6961, 48.2388, -90.4181, -82.4135),
        StateBoundingBox("MN", 43.4994, 49.3844, -97.2392, -89.4917),
        StateBoundingBox("MS", 30.1739, 34.9961, -91.6550, -88.0979),
        StateBoundingBox("MO", 35.9957, 40.6136, -95.7747, -89.0988),
        StateBoundingBox("MT", 44.3582, 49.0012, -116.0498, -104.0396),
        StateBoundingBox("NE", 40.0000, 43.0000, -104.0535, -95.3083),
        StateBoundingBox("NV", 35.0019, 42.0022, -120.0057, -114.0396),
        StateBoundingBox("NH", 42.6969, 45.3055, -72.5572, -70.6106),
        StateBoundingBox("NJ", 38.9285, 41.3574, -75.5598, -73.8939),
        StateBoundingBox("NM", 31.3323, 37.0002, -109.0502, -103.0020),
        StateBoundingBox("NY", 40.4961, 45.0159, -79.7622, -71.8562),
        StateBoundingBox("NC", 33.8423, 36.5881, -84.3219, -75.4606),
        StateBoundingBox("ND", 45.9351, 49.0007, -104.0489, -96.5544),
        StateBoundingBox("OH", 38.4032, 41.9775, -84.8202, -80.5186),
        StateBoundingBox("OK", 33.6158, 37.0023, -103.0026, -94.4307),
        StateBoundingBox("OR", 41.9918, 46.2920, -124.5662, -116.4635),
        StateBoundingBox("PA", 39.7198, 42.2699, -80.5199, -74.6895),
        StateBoundingBox("RI", 41.1463, 42.0188, -71.8627, -71.1206),
        StateBoundingBox("SC", 32.0346, 35.2154, -83.3539, -78.5411),
        StateBoundingBox("SD", 42.4796, 45.9457, -104.0579, -96.4366),
        StateBoundingBox("TN", 34.9829, 36.6781, -90.3103, -81.6469),
        StateBoundingBox("TX", 25.8371, 36.5007, -106.6456, -93.5083),
        StateBoundingBox("UT", 36.9979, 42.0016, -114.0529, -109.0411),
        StateBoundingBox("VT", 42.7269, 45.0167, -73.4377, -71.4646),
        StateBoundingBox("VA", 36.5407, 39.4660, -83.6754, -75.2423),
        StateBoundingBox("WA", 45.5435, 49.0025, -124.7631, -116.9160),
        StateBoundingBox("WV", 37.2015, 40.6388, -82.6447, -77.7195),
        StateBoundingBox("WI", 42.4919, 47.0768, -92.8881, -86.8054),
        StateBoundingBox("WY", 40.9947, 45.0059, -111.0569, -104.0522)
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
