package com.example.tmcloadtracker

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CurrentLoad::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun loadDao(): LoadDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Safe singleton factory constructor to control access to your data file
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tmc_loads_local.db" // The actual tiny file written to your phone's hardware
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
