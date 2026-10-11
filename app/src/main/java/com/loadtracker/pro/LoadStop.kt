package com.loadtracker.pro

import java.net.URLDecoder
import java.net.URLEncoder

data class LoadStop(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val dispatchedLegMiles: Double,
    val actualLegMiles: Double? = null
)

internal object LoadStopCodec {
    fun encode(stops: List<LoadStop>): String = stops.joinToString(";") { stop ->
        listOf(
            stop.name,
            stop.address,
            stop.latitude.toString(),
            stop.longitude.toString(),
            stop.dispatchedLegMiles.toString(),
            stop.actualLegMiles?.toString().orEmpty()
        ).joinToString(":") { value -> URLEncoder.encode(value, Charsets.UTF_8.name()) }
    }

    fun decode(encoded: String): List<LoadStop> {
        if (encoded.isBlank()) return emptyList()
        return encoded.split(';').map { row ->
            val fields = row.split(':')
            require(fields.size == 6) { "Invalid saved stop data." }
            val values = fields.map { value -> URLDecoder.decode(value, Charsets.UTF_8.name()) }
            LoadStop(
                name = values[0],
                address = values[1],
                latitude = requireNotNull(values[2].toDoubleOrNull()),
                longitude = requireNotNull(values[3].toDoubleOrNull()),
                dispatchedLegMiles = requireNotNull(values[4].toDoubleOrNull()),
                actualLegMiles = values[5].takeIf { it.isNotBlank() }?.toDoubleOrNull()
            )
        }
    }
}

internal data class LoadStopDraft(
    val name: String,
    val address: String,
    val latitude: Double?,
    val longitude: Double?,
    val dispatchedLegMiles: String,
    val actualLegMiles: Double? = null,
    val lookupError: String? = null,
    val isResolving: Boolean = false
) {
    fun toLoadStopOrNull(): LoadStop? {
        val lat = latitude ?: return null
        val long = longitude ?: return null
        val miles = dispatchedLegMiles.toDoubleOrNull() ?: return null
        if (name.isBlank() || address.isBlank() || miles <= 0.0) return null
        return LoadStop(name.trim(), address.trim(), lat, long, miles, actualLegMiles)
    }
}

internal fun CurrentLoad.recordLoadedArrival(
    cumulativeLoadedMiles: Double,
    arrivalTimestamp: Long
): CurrentLoad {
    val stops = LoadStopCodec.decode(stopOffsData)
    val nextStop = stops.getOrNull(currentStopIndex)
    val legMiles = (cumulativeLoadedMiles - lastStopLoadedMiles).coerceAtLeast(0.0)
    return if (nextStop != null) {
        val updatedStops = stops.toMutableList().apply {
            this[currentStopIndex] = nextStop.copy(actualLegMiles = legMiles)
        }
        copy(
            tripState = "ACTIVE_STOP",
            stopOffsData = LoadStopCodec.encode(updatedStops),
            currentStopIndex = currentStopIndex + 1,
            lastStopLoadedMiles = cumulativeLoadedMiles,
            loadedMilesEnd = cumulativeLoadedMiles,
            dockArrivalTime = arrivalTimestamp
        )
    } else {
        copy(
            tripState = "ACTIVE_CONSIGNEE",
            loadedMilesEnd = cumulativeLoadedMiles,
            dockArrivalTime = arrivalTimestamp
        )
    }
}

internal fun CurrentLoad.nextLoadedDestination(): LoadStop? =
    LoadStopCodec.decode(stopOffsData).getOrNull(currentStopIndex)

internal fun CurrentLoad.plannedCurrentLoadedLegMiles(): Double {
    val stop = nextLoadedDestination()
    if (stop != null) return stop.dispatchedLegMiles
    val totalStopLegMiles = LoadStopCodec.decode(stopOffsData).sumOf { it.dispatchedLegMiles }
    return (dispatchedLoadedMiles - totalStopLegMiles).coerceAtLeast(0.0)
}
