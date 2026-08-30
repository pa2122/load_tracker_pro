package com.example.tmcloadtracker

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
    val tripState: String
)
