package com.loadtracker.pro

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [CurrentLoad::class, TripBreadcrumb::class], version = 13, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {

    abstract fun loadDao(): LoadDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        internal val ALL_MIGRATIONS: Array<Migration>
            get() = arrayOf(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
                MIGRATION_9_10,
                MIGRATION_10_11,
                MIGRATION_11_12,
                MIGRATION_12_13
            )

        private fun hasTable(db: SupportSQLiteDatabase, tableName: String): Boolean {
            val cursor = db.query(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?",
                arrayOf(tableName)
            )
            return try {
                cursor.moveToFirst() && cursor.count > 0
            } finally {
                cursor.close()
            }
        }

        private fun hasColumn(db: SupportSQLiteDatabase, tableName: String, columnName: String): Boolean {
            val cursor = db.query(
                "PRAGMA table_info($tableName)",
                emptyArray()
            )
            return try {
                while (cursor.moveToNext()) {
                    val nameIndex = cursor.getColumnIndexOrThrow("name")
                    if (cursor.getString(nameIndex) == columnName) {
                        return true
                    }
                }
                false
            } finally {
                cursor.close()
            }
        }

        private fun addColumnIfMissing(db: SupportSQLiteDatabase, tableName: String, columnDefinition: String) {
            val columnName = columnDefinition.substringBefore(' ').trim()
            if (!hasColumn(db, tableName, columnName)) {
                db.execSQL("ALTER TABLE $tableName ADD COLUMN $columnDefinition")
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {}
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {}
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfMissing(db, "trucking_loads", "tripNotes TEXT")
                addColumnIfMissing(db, "trucking_loads", "deliveryTimestamp INTEGER")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfMissing(db, "trucking_loads", "shipperName TEXT")
                addColumnIfMissing(db, "trucking_loads", "shipperLat REAL")
                addColumnIfMissing(db, "trucking_loads", "shipperLong REAL")
                addColumnIfMissing(db, "trucking_loads", "consigneeName TEXT")
                addColumnIfMissing(db, "trucking_loads", "consigneeLat REAL")
                addColumnIfMissing(db, "trucking_loads", "consigneeLong REAL")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!hasTable(db, "trip_breadcrumbs")) {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS trip_breadcrumbs (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            proNumber TEXT NOT NULL,
                            latitude REAL NOT NULL,
                            longitude REAL NOT NULL,
                            timestamp INTEGER NOT NULL
                        )
                    """.trimIndent())
                }
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfMissing(db, "trucking_loads", "isTrainingWeek INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfMissing(db, "trucking_loads", "trainerPayRate REAL NOT NULL DEFAULT 0.0")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (hasTable(db, "trucking_loads")) {
                    db.execSQL("UPDATE trucking_loads SET shipperName = NULL WHERE LOWER(CAST(shipperName AS TEXT)) IN ('origin', 'pickup', 'shipper', '')")
                    db.execSQL("UPDATE trucking_loads SET consigneeName = NULL WHERE LOWER(CAST(consigneeName AS TEXT)) IN ('final dropoff', 'dropoff', 'drop', 'consignee', 'delivery', '')")
                }
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfMissing(db, "trucking_loads", "pickupApptText TEXT")
                addColumnIfMissing(db, "trucking_loads", "pickupApptTimestamp INTEGER")
                addColumnIfMissing(db, "trucking_loads", "pickupApptType TEXT")
                addColumnIfMissing(db, "trucking_loads", "consigneeApptText TEXT")
                addColumnIfMissing(db, "trucking_loads", "consigneeApptTimestamp INTEGER")
                addColumnIfMissing(db, "trucking_loads", "consigneeApptType TEXT")
                addColumnIfMissing(db, "trucking_loads", "dockArrivalTime INTEGER")
                addColumnIfMissing(db, "trucking_loads", "detentionHoursLogged REAL NOT NULL DEFAULT 0.0")
                addColumnIfMissing(db, "trucking_loads", "detentionFlatPay REAL NOT NULL DEFAULT 0.0")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfMissing(
                    db,
                    "trucking_loads",
                    "extraStopCount INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfMissing(db, "trucking_loads", "stopOffsData TEXT NOT NULL DEFAULT ''")
                addColumnIfMissing(db, "trucking_loads", "currentStopIndex INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(db, "trucking_loads", "lastStopLoadedMiles REAL NOT NULL DEFAULT 0.0")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            // Version 12's columns already exist; version 13 revalidates them against the corrected Room defaults.
            override fun migrate(db: SupportSQLiteDatabase) {}
        }

        // Safe singleton factory constructor to control access to your data file
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // Auto-migrate legacy file name tmc_loads_local.db -> load_tracker_local.db if needed
                val newDbFile = context.getDatabasePath("load_tracker_local.db")
                val oldDbFile = context.getDatabasePath("tmc_loads_local.db")
                if ((!newDbFile.exists() || newDbFile.length() < 1000) && oldDbFile.exists() && oldDbFile.length() > 1000) {
                    try {
                        oldDbFile.copyTo(newDbFile, overwrite = true)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "load_tracker_local.db"
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
