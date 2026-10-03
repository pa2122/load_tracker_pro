package com.loadtracker.pro

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dvir_entries")
data class DvirEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val truckNumber: String,
    val trailerNumber: String,
    val odometer: Double,
    val passedTractorCheck: Boolean,
    val passedCouplingCheck: Boolean,
    val passedBrakesTiresCheck: Boolean,
    val passedFlatbedGearCheck: Boolean,
    val strapsCount: Int,
    val chainsBindersCount: Int,
    val tarpsCondition: String, // "Good", "Needs Repair", "Missing"
    val coilRacksCount: Int,
    val defectsFound: String? = null,
    val isSafeToOperate: Boolean,
    val driverSignature: String
)
