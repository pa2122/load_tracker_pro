package com.example.tmcloadtracker

import android.content.Context
import android.os.Environment
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.File

@Database(entities = [CurrentLoad::class, TripBreadcrumb::class], version = 10, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {

    abstract fun loadDao(): LoadDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {}
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {}
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trucking_loads ADD COLUMN tripNotes TEXT")
                db.execSQL("ALTER TABLE trucking_loads ADD COLUMN deliveryTimestamp INTEGER")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trucking_loads ADD COLUMN shipperName TEXT")
                db.execSQL("ALTER TABLE trucking_loads ADD COLUMN shipperLat REAL")
                db.execSQL("ALTER TABLE trucking_loads ADD COLUMN shipperLong REAL")
                db.execSQL("ALTER TABLE trucking_loads ADD COLUMN consigneeName TEXT")
                db.execSQL("ALTER TABLE trucking_loads ADD COLUMN consigneeLat REAL")
                db.execSQL("ALTER TABLE trucking_loads ADD COLUMN consigneeLong REAL")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
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

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trucking_loads ADD COLUMN isTrainingWeek INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trucking_loads ADD COLUMN trainerPayRate REAL NOT NULL DEFAULT 0.0")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE trucking_loads SET shipperName = NULL WHERE LOWER(shipperName) IN ('origin', 'pickup', 'shipper', '')")
                db.execSQL("UPDATE trucking_loads SET consigneeName = NULL WHERE LOWER(consigneeName) IN ('final dropoff', 'dropoff', 'drop', 'consignee', 'delivery', '')")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trucking_loads ADD COLUMN pickupApptText TEXT")
                db.execSQL("ALTER TABLE trucking_loads ADD COLUMN pickupApptTimestamp INTEGER")
                db.execSQL("ALTER TABLE trucking_loads ADD COLUMN pickupApptType TEXT")
                db.execSQL("ALTER TABLE trucking_loads ADD COLUMN consigneeApptText TEXT")
                db.execSQL("ALTER TABLE trucking_loads ADD COLUMN consigneeApptTimestamp INTEGER")
                db.execSQL("ALTER TABLE trucking_loads ADD COLUMN consigneeApptType TEXT")
                db.execSQL("ALTER TABLE trucking_loads ADD COLUMN dockArrivalTime INTEGER")
                db.execSQL("ALTER TABLE trucking_loads ADD COLUMN detentionHoursLogged REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE trucking_loads ADD COLUMN detentionFlatPay REAL NOT NULL DEFAULT 0.0")
            }
        }

        private fun getSharedDatabasePath(context: Context): String {
            return try {
                val docDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "TMCLoadTracker")
                if (!docDir.exists()) {
                    docDir.mkdirs()
                }
                val sharedDb = File(docDir, "tmc_loads_shared.db")
                val oldMediaDb = File(context.getExternalFilesDir(null)?.parentFile?.parentFile?.parentFile, "media/com.example.tmcloadtracker/tmc_loads_shared.db")
                val oldAppDb = context.getDatabasePath("tmc_loads_local.db")

                if (!sharedDb.exists()) {
                    if (oldMediaDb.exists()) {
                        oldMediaDb.copyTo(sharedDb, overwrite = true)
                    } else if (oldAppDb.exists()) {
                        oldAppDb.copyTo(sharedDb, overwrite = true)
                    }
                }

                sharedDb.absolutePath
            } catch (_: Exception) {
                "tmc_loads_local.db"
            }
        }

        // Safe singleton factory constructor to control access to your data file
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    getSharedDatabasePath(context)
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
