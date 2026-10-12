package com.loadtracker.pro

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trucking_loads")
data class CurrentLoad(
    @PrimaryKey val proNumber: String,
    val dispatchedBounceMiles: Double,
    val dispatchedLoadedMiles: Double,
    val bounceMilesStart: Double,
    val bounceMilesEnd: Double,
    val loadedMilesStart: Double,
    val loadedMilesEnd: Double,
    val percentageRate: Double,
    val loadPay: Double,
    val tarpType: String,
    val isPreTarped: Boolean,
    val pickupTimestamp: Long,
    val isGoingHome: Boolean,
    val tripState: String,
    val tripNotes: String? = null,
    val deliveryTimestamp: Long? = null,
    // 📍 New fields added in Version 5 for Geofencing
    val shipperName: String? = null,
    val shipperLat: Double? = null,
    val shipperLong: Double? = null,
    val consigneeName: String? = null,
    val consigneeLat: Double? = null,
    val consigneeLong: Double? = null,
    // 📍 New field added in Version 7
    // Keep entity defaults aligned with migration SQL or Room throws "Migration didn't properly handle: trucking_loads" at launch.
    @ColumnInfo(defaultValue = "0")
    val isTrainingWeek: Boolean = false,
    // 📍 New field added in Version 8
    @ColumnInfo(defaultValue = "0.0")
    val trainerPayRate: Double = 0.0,
    // 📍 New fields added in Version 10 for Appointment & Detention Clock
    val pickupApptText: String? = null,
    val pickupApptTimestamp: Long? = null,
    val pickupApptType: String? = null,
    val consigneeApptText: String? = null,
    val consigneeApptTimestamp: Long? = null,
    val consigneeApptType: String? = null,
    val dockArrivalTime: Long? = null,
    @ColumnInfo(defaultValue = "0.0")
    val detentionHoursLogged: Double = 0.0,
    @ColumnInfo(defaultValue = "0.0")
    val detentionFlatPay: Double = 0.0,
    @ColumnInfo(defaultValue = "0")
    val extraStopCount: Int = 0,
    @ColumnInfo(defaultValue = "''")
    val stopOffsData: String = "",
    @ColumnInfo(defaultValue = "0")
    val currentStopIndex: Int = 0,
    @ColumnInfo(defaultValue = "0.0")
    val lastStopLoadedMiles: Double = 0.0
)

internal fun CurrentLoad.extraStopPayCents(ratePerStop: Double): Long =
    Math.round(ratePerStop * 100.0) * extraStopCount.toLong()

@Entity(tableName = "trip_breadcrumbs")
data class TripBreadcrumb(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val proNumber: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)
