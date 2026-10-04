package com.loadtracker.pro

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileOutputStream

class DatabaseBackupTest {

    @Test
    fun test1_DatabaseBackup_ValidatesSqlite3FileHeaderCorrectly() {
        val tempValidDb = File.createTempFile("test_valid", ".db").apply {
            FileOutputStream(this).use {
                it.write("SQLite format 3\u0000HeaderSampleBytesForTestValidationData".toByteArray())
            }
        }

        val isValid = DatabaseBackupManager.isValidSqliteDatabase(tempValidDb)
        assertTrue("Valid SQLite file should pass header check", isValid)

        tempValidDb.delete()
    }

    @Test
    fun test2_DatabaseBackup_RejectsInvalidNonSqliteFileHeader() {
        val tempInvalidFile = File.createTempFile("test_invalid", ".txt").apply {
            FileOutputStream(this).use {
                it.write("Invalid plain text file content".toByteArray())
            }
        }

        val isValid = DatabaseBackupManager.isValidSqliteDatabase(tempInvalidFile)
        assertFalse("Invalid non-SQLite file should fail header check", isValid)

        tempInvalidFile.delete()
    }
}
