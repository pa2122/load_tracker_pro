package com.example.tmcloadtracker

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LoadDao {

    // Saves a load record. If the Pro # already exists, it cleanly overwrites it with your new edits.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoad(load: CurrentLoad)

    // Fetches all past records from your phone's drive, sorting the newest loads to the top by pickup date.
    @Query("SELECT * FROM trucking_loads ORDER BY pickupTimestamp DESC")
    fun getAllLoads(): Flow<List<CurrentLoad>>

    // Deletes an entry permanently from your phone's local storage file.
    @Delete
    suspend fun deleteLoad(load: CurrentLoad)

    @Query("UPDATE trucking_loads SET tripState = :newState WHERE proNumber = :pro")
    suspend fun updateTripState(pro: String, newState: String)

    @Insert
    suspend fun insertBreadcrumb(breadcrumb: TripBreadcrumb)

    @Query("SELECT * FROM trip_breadcrumbs WHERE proNumber = :pro ORDER BY timestamp ASC")
    fun getBreadcrumbsForLoad(pro: String): Flow<List<TripBreadcrumb>>

    @Query("SELECT * FROM trip_breadcrumbs ORDER BY timestamp ASC")
    fun getAllBreadcrumbs(): Flow<List<TripBreadcrumb>>

    @Query("DELETE FROM trip_breadcrumbs")
    suspend fun clearAllBreadcrumbs()

    @Query("SELECT COUNT(*) FROM trip_breadcrumbs")
    fun getBreadcrumbCount(): Flow<Int>

    // ⛽ Fuel & IFTA Logger Queries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelEntry(entry: FuelEntry)

    @Query("SELECT * FROM fuel_entries ORDER BY timestamp DESC")
    fun getAllFuelEntries(): Flow<List<FuelEntry>>

    @Delete
    suspend fun deleteFuelEntry(entry: FuelEntry)

    @Query("DELETE FROM fuel_entries")
    suspend fun clearAllFuelEntries()

    @Query("""
        SELECT DISTINCT shipperName FROM trucking_loads 
        WHERE shipperName IS NOT NULL AND LOWER(shipperName) NOT IN ('origin', 'pickup', 'shipper', '')
        UNION 
        SELECT DISTINCT consigneeName FROM trucking_loads 
        WHERE consigneeName IS NOT NULL AND LOWER(consigneeName) NOT IN ('final dropoff', 'dropoff', 'drop', 'consignee', 'delivery', '')
    """)
    fun getAllFacilityNames(): Flow<List<String>>

    @Query("SELECT tripNotes, pickupTimestamp, proNumber FROM trucking_loads WHERE (shipperName = :name OR consigneeName = :name) AND tripNotes IS NOT NULL")
    fun getNotesForFacility(name: String): Flow<List<FacilityNote>>
}

data class FacilityNote(
    val tripNotes: String,
    val pickupTimestamp: Long,
    val proNumber: String
)
