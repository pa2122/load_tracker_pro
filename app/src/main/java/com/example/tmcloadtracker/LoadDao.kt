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

    // Fetches all past records from your phone's drive, sorting the newest loads to the top.
    @Query("SELECT * FROM trucking_loads ORDER BY proNumber DESC")
    fun getAllLoads(): Flow<List<CurrentLoad>>

    // Deletes an entry permanently from your phone's local storage file.
    @Delete
    suspend fun deleteLoad(load: CurrentLoad)
}
