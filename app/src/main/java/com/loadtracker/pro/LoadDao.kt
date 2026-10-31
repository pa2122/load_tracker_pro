package com.loadtracker.pro

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

    @Query("SELECT * FROM trucking_loads WHERE proNumber = :pro")
    suspend fun getLoadByProNumber(pro: String): CurrentLoad?

    @Query(
        """
        UPDATE trucking_loads
        SET tripState = :newState,
            bounceMilesEnd = CASE WHEN :newState = 'ACTIVE_SHIPPER' THEN :bounceMilesEnd ELSE bounceMilesEnd END,
            loadedMilesEnd = CASE WHEN :newState = 'ACTIVE_CONSIGNEE' THEN :loadedMilesEnd ELSE loadedMilesEnd END,
            dockArrivalTime = :dockArrivalTime
        WHERE proNumber = :pro AND tripState = :expectedState
        """
    )
    suspend fun updateTripFromNotification(
        pro: String,
        expectedState: String,
        newState: String,
        bounceMilesEnd: Double,
        loadedMilesEnd: Double,
        dockArrivalTime: Long?
    ): Int

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

    // 📋 Pre-Trip DVIR Queries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDvirEntry(entry: DvirEntry)

    @Query("SELECT * FROM dvir_entries ORDER BY timestamp DESC")
    fun getAllDvirEntries(): Flow<List<DvirEntry>>

    @Delete
    suspend fun deleteDvirEntry(entry: DvirEntry)

    @Query("SELECT * FROM facility_notes ORDER BY displayName COLLATE NOCASE")
    fun getAllFacilityNotes(): Flow<List<FacilityNoteEntity>>

    @Query("SELECT * FROM facility_notes WHERE facilityKey = :key")
    suspend fun getFacilityNote(key: String): FacilityNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFacilityNote(note: FacilityNoteEntity)
}
