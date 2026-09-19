package com.example.tmcloadtracker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fuel_entries")
data class FuelEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val gallons: Double,
    val totalCost: Double,
    val pricePerGallon: Double,
    val state: String,
    val stationName: String,
    val odometer: Double
)
