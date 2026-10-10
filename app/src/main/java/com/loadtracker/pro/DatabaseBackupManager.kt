package com.loadtracker.pro

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DatabaseBackupManager {

    private const val DB_NAME_V1 = "tmc_loads_local.db"
    private const val DB_NAME_V2 = "load_tracker_local.db"

    data class BackupMetadata(
        val fileName: String,
        val formattedDate: String,
        val sizeKb: Long,
        val isValid: Boolean
    )

    /**
     * Finds active SQLite database file on device.
     */
    fun getActiveDatabaseFile(context: Context): File {
        val db1 = context.getDatabasePath(DB_NAME_V1)
        if (db1.exists()) return db1
        return context.getDatabasePath(DB_NAME_V2)
    }

    /**
     * Validates if a file is a valid SQLite 3 database by checking its header.
     */
    fun isValidSqliteDatabase(file: File): Boolean {
        if (!file.exists() || file.length() < 16) return false
        return try {
            val header = ByteArray(16)
            FileInputStream(file).use { it.read(header) }
            String(header, Charsets.US_ASCII).startsWith("SQLite format 3")
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Finds the latest local backup file in app files directory.
     */
    fun getLatestBackupFile(context: Context): File? {
        val backupDir = File(context.filesDir, "backups")
        if (!backupDir.exists()) return null
        return backupDir.listFiles { _, name -> name.startsWith("load_tracker_backup_") && name.endsWith(".db") }
            ?.maxByOrNull { it.lastModified() }
    }

    /**
     * Extracts metadata preview from a backup file.
     */
    fun getBackupMetadata(file: File?): BackupMetadata? {
        if (file == null || !file.exists()) return null
        val isValid = isValidSqliteDatabase(file)
        val dateStr = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.US).format(Date(file.lastModified()))
        val kb = file.length() / 1024
        return BackupMetadata(
            fileName = file.name,
            formattedDate = dateStr,
            sizeKb = kb,
            isValid = isValid
        )
    }

    /**
     * Creates a timestamped backup copy of the Room database.
     */
    fun createDatabaseBackup(context: Context): Pair<File?, String> {
        try {
            val activeDb = getActiveDatabaseFile(context)

            // Force SQLite WAL checkpoint to flush journal logs into main DB file
            try {
                val db = AppDatabase.getDatabase(context)
                val supportDb = db.openHelper.writableDatabase
                supportDb.query("PRAGMA wal_checkpoint(FULL)").close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (!activeDb.exists()) {
                return Pair(null, "Active database file not found.")
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val backupFileName = "load_tracker_backup_$timestamp.db"
            val backupDir = File(context.filesDir, "backups").apply { mkdirs() }
            val backupFile = File(backupDir, backupFileName)

            FileInputStream(activeDb).use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                }
            }

            return Pair(backupFile, "Backup created successfully: $backupFileName")
        } catch (e: Exception) {
            return Pair(null, "Backup failed: ${e.localizedMessage}")
        }
    }

    /**
     * Gets a FileProvider content URI for sharing backup file via Android Share Sheet.
     */
    fun getShareableUri(context: Context, backupFile: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            backupFile
        )
    }

    /**
     * Restores database from a selected backup file URI.
     */
    fun restoreDatabase(context: Context, backupUri: Uri): Pair<Boolean, String> {
        try {
            val activeDb = getActiveDatabaseFile(context)
            val tempRestoreFile = File(context.cacheDir, "temp_restore.db")

            context.contentResolver.openInputStream(backupUri)?.use { input ->
                FileOutputStream(tempRestoreFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (!isValidSqliteDatabase(tempRestoreFile)) {
                return Pair(false, "Invalid database file. Header does not match SQLite 3 format.")
            }

            // Close active Room database instance
            try {
                AppDatabase.getDatabase(context).close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Overwrite active DB file
            FileInputStream(tempRestoreFile).use { input ->
                FileOutputStream(activeDb).use { output ->
                    input.copyTo(output)
                }
            }

            tempRestoreFile.delete()
            return Pair(true, "Database restored successfully! Restart app to refresh records.")
        } catch (e: Exception) {
            return Pair(false, "Database restore failed: ${e.localizedMessage}")
        }
    }
}
