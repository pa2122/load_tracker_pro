package com.loadtracker.pro

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    @Test
    fun migratesVersion3DatabaseToCurrentSchemaWithoutLosingLoadData() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "migration_v3_to_current_test.db"
        context.deleteDatabase(databaseName)

        val legacyFile = context.getDatabasePath(databaseName)
        legacyFile.parentFile?.mkdirs()
        val legacyDatabase = SQLiteDatabase.openOrCreateDatabase(legacyFile, null)
        try {
            legacyDatabase.execSQL(
                """
                CREATE TABLE trucking_loads (
                    proNumber TEXT NOT NULL PRIMARY KEY,
                    dispatchedBounceMiles REAL NOT NULL,
                    dispatchedLoadedMiles REAL NOT NULL,
                    bounceMilesStart REAL NOT NULL,
                    bounceMilesEnd REAL NOT NULL,
                    loadedMilesStart REAL NOT NULL,
                    loadedMilesEnd REAL NOT NULL,
                    percentageRate REAL NOT NULL,
                    loadPay REAL NOT NULL,
                    tarpType TEXT NOT NULL,
                    isPreTarped INTEGER NOT NULL,
                    pickupTimestamp INTEGER NOT NULL,
                    isGoingHome INTEGER NOT NULL,
                    tripState TEXT NOT NULL
                )
                """.trimIndent()
            )
            legacyDatabase.execSQL(
                """
                INSERT INTO trucking_loads (
                    proNumber, dispatchedBounceMiles, dispatchedLoadedMiles,
                    bounceMilesStart, bounceMilesEnd, loadedMilesStart, loadedMilesEnd,
                    percentageRate, loadPay, tarpType, isPreTarped, pickupTimestamp,
                    isGoingHome, tripState
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any>("MIGRATION-TEST-1", 12.5, 300.0, 0.0, 12.5, 0.0, 300.0, 31.0, 1000.0, "S", 0, 123456789L, 0, "ACTIVE_BOUNCE")
            )
            legacyDatabase.version = 3
        } finally {
            legacyDatabase.close()
        }

        val database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        try {
            assertEquals(10, database.openHelper.writableDatabase.version)

            val migratedLoad = database.loadDao().getAllLoads().first().single()
            assertEquals("MIGRATION-TEST-1", migratedLoad.proNumber)
            assertEquals(12.5, migratedLoad.dispatchedBounceMiles, 0.0)
            assertEquals(300.0, migratedLoad.dispatchedLoadedMiles, 0.0)
            assertEquals(1000.0, migratedLoad.loadPay, 0.0)

            database.loadDao().insertBreadcrumb(
                TripBreadcrumb(
                    proNumber = migratedLoad.proNumber,
                    latitude = 32.0,
                    longitude = -96.0,
                    timestamp = 123456790L
                )
            )
            assertEquals(
                1,
                database.loadDao().getBreadcrumbsForLoad(migratedLoad.proNumber).first().size
            )
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }
}
