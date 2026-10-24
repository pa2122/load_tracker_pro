package com.loadtracker.pro

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LoadDao {

    // Saves a load record. If the Pro # already exists, it cleanly overwrites it with your new edits.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoad(load: CurrentLoad)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLoadsIgnoringConflicts(loads: List<CurrentLoad>): List<Long>

    @Query("SELECT proNumber FROM trucking_loads")
    suspend fun getAllProNumbers(): List<String>

    @Transaction
    suspend fun insertHistoricalBatchIfNoConflicts(loads: List<CurrentLoad>) {
        require(loads.isNotEmpty()) { "The import batch is empty." }
        val batchNumbers = loads.map { it.proNumber }
        require(batchNumbers.distinct().size == batchNumbers.size) {
            "The import batch contains duplicate PRO numbers."
        }
        val existing = getAllProNumbers().toSet()
        val conflict = batchNumbers.firstOrNull { it in existing }
        check(conflict == null) { "PRO #$conflict already exists. No loads were imported." }

        val insertedIds = insertLoadsIgnoringConflicts(loads)
        check(insertedIds.size == loads.size && insertedIds.none { it == -1L }) {
            "A PRO number conflict prevented the batch from being fully imported."
        }
    }

    // Fetches all past records from your phone's drive, sorting the newest loads to the top by pickup date.
    @Query("SELECT * FROM trucking_loads ORDER BY pickupTimestamp DESC")
    fun getAllLoads(): Flow<List<CurrentLoad>>

    @Query("SELECT * FROM trucking_loads WHERE proNumber = :pro LIMIT 1")
    suspend fun getLoadByProNumber(pro: String): CurrentLoad?

    @Update
    suspend fun updateLoadIfPresent(load: CurrentLoad): Int

    @Transaction
    suspend fun updateTripFromNotification(
        expectedState: String,
        updatedLoad: CurrentLoad
    ): Int {
        val currentLoad = getLoadByProNumber(updatedLoad.proNumber) ?: return 0
        if (currentLoad.tripState != expectedState) return 0
        return updateLoadIfPresent(updatedLoad)
    }

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

    @Query("""
        SELECT DISTINCT shipperName FROM trucking_loads 
        WHERE shipperName IS NOT NULL AND LOWER(shipperName) NOT IN ('origin', 'pickup', 'shipper', '')
        UNION 
        SELECT DISTINCT consigneeName FROM trucking_loads 
        WHERE consigneeName IS NOT NULL AND LOWER(consigneeName) NOT IN ('final dropoff', 'dropoff', 'drop', 'consignee', 'delivery', '')
    """)
    fun getAllFacilityNames(): Flow<List<String>>

    @Query("SELECT tripNotes, pickupTimestamp, proNumber, shipperName, consigneeName FROM trucking_loads WHERE (shipperName = :name OR consigneeName = :name) AND tripNotes IS NOT NULL AND TRIM(tripNotes) != ''")
    fun getNotesForFacility(name: String): Flow<List<FacilityNote>>
}

data class FacilityNote(
    val tripNotes: String,
    val pickupTimestamp: Long,
    val proNumber: String,
    val shipperName: String,
    val consigneeName: String
)
