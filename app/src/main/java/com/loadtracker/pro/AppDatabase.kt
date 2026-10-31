package com.loadtracker.pro

import android.content.Context
import android.os.Environment
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.File

@Database(entities = [CurrentLoad::class, TripBreadcrumb::class, FuelEntry::class, DvirEntry::class, FacilityNoteEntity::class], version = 15, exportSchema = true)
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
                MIGRATION_12_13,
                MIGRATION_13_14,
                MIGRATION_14_15
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

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS fuel_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        gallons REAL NOT NULL,
                        totalCost REAL NOT NULL,
                        pricePerGallon REAL NOT NULL,
                        state TEXT NOT NULL,
                        stationName TEXT NOT NULL,
                        odometer REAL NOT NULL
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS dvir_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        truckNumber TEXT NOT NULL,
                        trailerNumber TEXT NOT NULL,
                        odometer REAL NOT NULL,
                        passedTractorCheck INTEGER NOT NULL,
                        passedCouplingCheck INTEGER NOT NULL,
                        passedBrakesTiresCheck INTEGER NOT NULL,
                        passedFlatbedGearCheck INTEGER NOT NULL,
                        strapsCount INTEGER NOT NULL,
                        chainsBindersCount INTEGER NOT NULL,
                        tarpsCondition TEXT NOT NULL,
                        coilRacksCount INTEGER NOT NULL,
                        defectsFound TEXT,
                        isSafeToOperate INTEGER NOT NULL,
                        driverSignature TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Databases that came from v1.x (same version numbers, no fuel table) get it in 14->15.
                if (hasTable(db, "fuel_entries")) {
                    addColumnIfMissing(db, "fuel_entries", "defGallons REAL NOT NULL DEFAULT 0.0")
                    addColumnIfMissing(db, "fuel_entries", "defCost REAL NOT NULL DEFAULT 0.0")
                }
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (hasTable(db, "fuel_entries")) {
                    addColumnIfMissing(db, "fuel_entries", "receiptPhotoUri TEXT")
                }
            }
        }

        private fun ensureFuelAndDvirTables(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS fuel_entries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    gallons REAL NOT NULL,
                    totalCost REAL NOT NULL,
                    pricePerGallon REAL NOT NULL,
                    state TEXT NOT NULL,
                    stationName TEXT NOT NULL,
                    odometer REAL NOT NULL,
                    defGallons REAL NOT NULL DEFAULT 0.0,
                    defCost REAL NOT NULL DEFAULT 0.0,
                    receiptPhotoUri TEXT
                )
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS dvir_entries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    truckNumber TEXT NOT NULL,
                    trailerNumber TEXT NOT NULL,
                    odometer REAL NOT NULL,
                    passedTractorCheck INTEGER NOT NULL,
                    passedCouplingCheck INTEGER NOT NULL,
                    passedBrakesTiresCheck INTEGER NOT NULL,
                    passedFlatbedGearCheck INTEGER NOT NULL,
                    strapsCount INTEGER NOT NULL,
                    chainsBindersCount INTEGER NOT NULL,
                    tarpsCondition TEXT NOT NULL,
                    coilRacksCount INTEGER NOT NULL,
                    defectsFound TEXT,
                    isSafeToOperate INTEGER NOT NULL,
                    driverSignature TEXT NOT NULL
                )
            """.trimIndent())
        }

        // v1.x databases carry multi-stop columns that v2's loads table does not have; Room rejects extras, so rebuild.
        private fun dropV1OnlyLoadColumns(db: SupportSQLiteDatabase) {
            if (!hasTable(db, "trucking_loads")) return
            val v1Only = listOf("extraStopCount", "stopOffsData", "currentStopIndex", "lastStopLoadedMiles")
            if (v1Only.none { hasColumn(db, "trucking_loads", it) }) return
            val columns = "proNumber, dispatchedBounceMiles, dispatchedLoadedMiles, bounceMilesStart, bounceMilesEnd, " +
                "loadedMilesStart, loadedMilesEnd, percentageRate, loadPay, tarpType, isPreTarped, pickupTimestamp, " +
                "isGoingHome, tripState, tripNotes, deliveryTimestamp, shipperName, shipperLat, shipperLong, " +
                "consigneeName, consigneeLat, consigneeLong, isTrainingWeek, trainerPayRate, pickupApptText, " +
                "pickupApptTimestamp, pickupApptType, consigneeApptText, consigneeApptTimestamp, consigneeApptType, " +
                "dockArrivalTime, detentionHoursLogged, detentionFlatPay"
            db.execSQL("DROP TABLE IF EXISTS trucking_loads_v2_rebuild")
            db.execSQL(
                "CREATE TABLE trucking_loads_v2_rebuild (`proNumber` TEXT NOT NULL, `dispatchedBounceMiles` REAL NOT NULL, " +
                    "`dispatchedLoadedMiles` REAL NOT NULL, `bounceMilesStart` REAL NOT NULL, `bounceMilesEnd` REAL NOT NULL, " +
                    "`loadedMilesStart` REAL NOT NULL, `loadedMilesEnd` REAL NOT NULL, `percentageRate` REAL NOT NULL, " +
                    "`loadPay` REAL NOT NULL, `tarpType` TEXT NOT NULL, `isPreTarped` INTEGER NOT NULL, " +
                    "`pickupTimestamp` INTEGER NOT NULL, `isGoingHome` INTEGER NOT NULL, `tripState` TEXT NOT NULL, " +
                    "`tripNotes` TEXT, `deliveryTimestamp` INTEGER, `shipperName` TEXT, `shipperLat` REAL, `shipperLong` REAL, " +
                    "`consigneeName` TEXT, `consigneeLat` REAL, `consigneeLong` REAL, `isTrainingWeek` INTEGER NOT NULL, " +
                    "`trainerPayRate` REAL NOT NULL, `pickupApptText` TEXT, `pickupApptTimestamp` INTEGER, " +
                    "`pickupApptType` TEXT, `consigneeApptText` TEXT, `consigneeApptTimestamp` INTEGER, " +
                    "`consigneeApptType` TEXT, `dockArrivalTime` INTEGER, `detentionHoursLogged` REAL NOT NULL, " +
                    "`detentionFlatPay` REAL NOT NULL, PRIMARY KEY(`proNumber`))"
            )
            db.execSQL("INSERT INTO trucking_loads_v2_rebuild ($columns) SELECT $columns FROM trucking_loads")
            db.execSQL("DROP TABLE trucking_loads")
            db.execSQL("ALTER TABLE trucking_loads_v2_rebuild RENAME TO trucking_loads")
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            // One notes record per facility (name + address); backfilled from the notes blobs saved on past loads.
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureFuelAndDvirTables(db)
                dropV1OnlyLoadColumns(db)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS facility_notes (
                        facilityKey TEXT NOT NULL,
                        displayName TEXT NOT NULL,
                        instructions TEXT NOT NULL,
                        directions TEXT NOT NULL,
                        parking TEXT NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(facilityKey)
                    )
                    """.trimIndent()
                )
                if (hasTable(db, "trucking_loads") &&
                    hasColumn(db, "trucking_loads", "tripNotes") &&
                    hasColumn(db, "trucking_loads", "shipperName") &&
                    hasColumn(db, "trucking_loads", "consigneeName")
                ) {
                    try {
                        backfillFacilityNotes(db)
                    } catch (e: Exception) {
                        // Notes are a convenience; never block the upgrade over unparseable history.
                        e.printStackTrace()
                    }
                }
            }
        }

        private fun backfillFacilityNotes(db: SupportSQLiteDatabase) {
            val placeholders = setOf(
                "origin", "pickup", "shipper", "final dropoff", "dropoff", "drop", "consignee", "delivery"
            )
            val merged = linkedMapOf<String, FacilityNoteEntity>()

            fun absorb(name: String?, fields: FacilityNoteFields, timestamp: Long) {
                val displayName = name?.trim().orEmpty()
                if (displayName.isEmpty() || displayName.lowercase() in placeholders || fields.isEmpty) return
                val key = FacilityNotes.key(displayName)
                if (key.isEmpty()) return
                val combined = merged[key]?.let { FacilityNotes.merge(it.fields(), fields) } ?: fields
                merged[key] = FacilityNoteEntity(
                    facilityKey = key,
                    displayName = displayName,
                    instructions = combined.instructions,
                    directions = combined.directions,
                    parking = combined.parking,
                    updatedAt = timestamp
                )
            }

            // Oldest first so the newest load's notes win.
            val cursor = db.query(
                "SELECT shipperName, consigneeName, tripNotes, pickupTimestamp FROM trucking_loads " +
                    "WHERE tripNotes IS NOT NULL AND TRIM(tripNotes) != '' ORDER BY pickupTimestamp ASC"
            )
            try {
                while (cursor.moveToNext()) {
                    val (shipperFields, consigneeFields) = FacilityNotes.parseTripNotes(cursor.getString(2).orEmpty())
                    val timestamp = cursor.getLong(3)
                    absorb(cursor.getString(0), shipperFields, timestamp)
                    absorb(cursor.getString(1), consigneeFields, timestamp)
                }
            } finally {
                cursor.close()
            }

            merged.values.forEach { note ->
                db.insert(
                    "facility_notes",
                    android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE,
                    android.content.ContentValues().apply {
                        put("facilityKey", note.facilityKey)
                        put("displayName", note.displayName)
                        put("instructions", note.instructions)
                        put("directions", note.directions)
                        put("parking", note.parking)
                        put("updatedAt", note.updatedAt)
                    }
                )
            }
        }

        // Safe singleton factory constructor to control access to your data file
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "load_tracker_local.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
