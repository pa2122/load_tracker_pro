package com.loadtracker.pro

import android.content.Context
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class HomeAddressResolution(
    val latitude: Double,
    val longitude: Double,
    val displayName: String?
)

data class HomeAddressQuery(
    val address: String,
    val displayName: String?
)

object HomeAddressResolver {

    internal fun createQuery(input: String): HomeAddressQuery? {
        val lines = input.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return null

        return HomeAddressQuery(
            address = lines.joinToString(separator = ", ").trim(),
            displayName = lines.first().trim().takeIf { lines.size > 1 }
        )
    }

    suspend fun resolve(context: Context, query: HomeAddressQuery): HomeAddressResolution? =
        withContext(Dispatchers.IO) {
            val geocoder = Geocoder(context.applicationContext, Locale.US)
            @Suppress("DEPRECATION")
            val location = geocoder.getFromLocationName(query.address, 1)?.firstOrNull()
                ?: return@withContext null

            HomeAddressResolution(
                latitude = location.latitude,
                longitude = location.longitude,
                displayName = query.displayName
            )
        }
}
