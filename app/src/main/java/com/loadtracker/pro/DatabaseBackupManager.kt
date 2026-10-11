package com.loadtracker.pro

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DatabaseBackupManager {

    private const val DB_NAME_V1 = "tmc_loads_local.db"
    private const val DB_NAME_V2 = "load_tracker_local.db"
    private const val AUTO_BACKUP_PREFIX = "load_tracker_auto_backup_"
    private const val AUTO_BACKUP_SUFFIX = ".db"
    private const val AUTO_BACKUP_RETENTION_DAYS = 14
    const val AUTO_BACKUP_ERROR_KEY = "automatic_backup_error"
    private val automaticBackupLock = Any()

    data class BackupMetadata(
        val fileName: String,
        val formattedDate: String,
        val sizeKb: Long,
        val absolutePath: String,
        val isValid: Boolean,
        val file: File
    )

    /**
     * Finds active SQLite database file on device.
     */
    fun getActiveDatabaseFile(context: Context): File {
        val currentDatabase = context.getDatabasePath(DB_NAME_V2)
        if (currentDatabase.exists()) return currentDatabase
        return context.getDatabasePath(DB_NAME_V1)
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
     * Finds all local backup files in app files directory sorted newest-first.
     */
    fun getAllBackupFiles(context: Context): List<File> {
        val backupDir = File(context.filesDir, "backups")
        if (!backupDir.exists()) return emptyList()
        val manualFiles = backupDir.listFiles { _, name ->
            name.startsWith("load_tracker_backup_") && name.endsWith(".db")
        }?.toList().orEmpty()
        return (manualFiles + getAutomaticBackupFiles(context))
            .sortedByDescending { it.lastModified() }
    }

    /**
     * Finds the latest local backup file in app files directory.
     */
    fun getLatestBackupFile(context: Context): File? {
        return File(context.filesDir, "backups").listFiles { _, name ->
            name.startsWith("load_tracker_backup_") && name.endsWith(".db")
        }?.maxByOrNull { it.lastModified() }
    }

    fun getLatestAutomaticBackupFile(context: Context): File? =
        getAutomaticBackupFiles(context).firstOrNull()

    private fun getAutomaticBackupFiles(context: Context): List<File> {
        val backupDir = File(context.filesDir, "backups")
        return backupDir.listFiles { _, name ->
            name.startsWith(AUTO_BACKUP_PREFIX) && name.endsWith(AUTO_BACKUP_SUFFIX)
        }?.sortedByDescending { it.name } ?: emptyList()
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
            absolutePath = file.absolutePath,
            isValid = isValid,
            file = file
        )
    }

    /**
     * Creates a timestamped backup copy of the Room database.
     * When replacePrevious is true, deletes ONLY the single most recent backup file (preserving older backups).
     */
    fun createDatabaseBackup(context: Context, replacePrevious: Boolean = false): Pair<File?, String> {
        try {
            val activeDb = getActiveDatabaseFile(context)

            checkpointDatabase(context)

            if (!activeDb.exists()) {
                return Pair(null, "Active database file not found.")
            }

            val backupDir = File(context.filesDir, "backups").apply { mkdirs() }

            if (replacePrevious) {
                // Delete ONLY the single most recent backup file (preserving older historical backups!)
                val latest = getLatestBackupFile(context)
                latest?.delete()
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val backupFileName = "load_tracker_backup_$timestamp.db"
            val backupFile = File(backupDir, backupFileName)

            FileInputStream(activeDb).use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                }
            }

            return Pair(backupFile, "Backup saved to: ${backupFile.absolutePath}")
        } catch (e: Exception) {
            return Pair(null, "Backup failed: ${e.localizedMessage}")
        }
    }

    /**
     * Saves one private on-device snapshot immediately before the first database write each day.
     * Automatic snapshots are kept separately from manually created/exported backups.
     */
    suspend fun createDailyBackupBeforeMutation(context: Context): Boolean = withContext(Dispatchers.IO) {
        synchronized(automaticBackupLock) {
            val today = LocalDate.now().toString().replace("-", "")
            val backupDir = File(context.filesDir, "backups")
            val backupFile = File(backupDir, "$AUTO_BACKUP_PREFIX$today$AUTO_BACKUP_SUFFIX")
            if (backupFile.exists() && isValidSqliteDatabase(backupFile)) {
                context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                    .edit().remove(AUTO_BACKUP_ERROR_KEY).apply()
                return@synchronized true
            }

            try {
                check(backupDir.exists() || backupDir.mkdirs()) { "Unable to create the local backups directory." }
                checkpointDatabase(context)
                val activeDb = getActiveDatabaseFile(context)
                check(activeDb.exists()) { "Active database file not found." }

                val tempFile = File(backupDir, "${backupFile.name}.tmp")
                FileInputStream(activeDb).use { input ->
                    FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                }
                check(isValidSqliteDatabase(tempFile)) { "Automatic backup failed SQLite validation." }
                check(!backupFile.exists() || backupFile.delete()) {
                    "Could not replace the invalid automatic backup."
                }
                check(tempFile.renameTo(backupFile)) { "Unable to finalize the automatic backup file." }

                val cutoff = LocalDate.now().minusDays(AUTO_BACKUP_RETENTION_DAYS.toLong() - 1)
                    .format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE)
                val automaticFiles = getAutomaticBackupFiles(context)
                automaticFiles.forEach { file ->
                    val date = file.name.removePrefix(AUTO_BACKUP_PREFIX).removeSuffix(AUTO_BACKUP_SUFFIX)
                    if (date < cutoff && !file.delete()) {
                        Log.w("DatabaseBackupManager", "Could not prune old automatic backup ${file.name}")
                    }
                }
                getAutomaticBackupFiles(context).drop(AUTO_BACKUP_RETENTION_DAYS).forEach { file ->
                    if (!file.delete()) {
                        Log.w("DatabaseBackupManager", "Could not prune excess automatic backup ${file.name}")
                    }
                }
                context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                    .edit().remove(AUTO_BACKUP_ERROR_KEY).apply()
                true
            } catch (e: Exception) {
                Log.e("DatabaseBackupManager", "Could not create automatic local database backup", e)
                context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                    .edit().putString(
                        AUTO_BACKUP_ERROR_KEY,
                        e.localizedMessage ?: e.javaClass.simpleName
                    ).apply()
                false
            }
        }
    }

    private fun checkpointDatabase(context: Context) {
        val supportDb = AppDatabase.getDatabase(context).openHelper.writableDatabase
        supportDb.query("PRAGMA wal_checkpoint(FULL)").use { cursor ->
            if (cursor.moveToFirst() && cursor.getInt(0) != 0) {
                error("SQLite could not checkpoint the database WAL.")
            }
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
     * Restores database directly from a local File instance.
     */
    fun restoreDatabaseFromFile(context: Context, backupFile: File): Pair<Boolean, String> {
        if (!backupFile.exists() || !isValidSqliteDatabase(backupFile)) {
            return Pair(false, "Invalid database file.")
        }
        return try {
            val activeDb = getActiveDatabaseFile(context)
            try {
                AppDatabase.getDatabase(context).close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            FileInputStream(backupFile).use { input ->
                FileOutputStream(activeDb).use { output ->
                    input.copyTo(output)
                }
            }
            Pair(true, "Database restored successfully from ${backupFile.name}! Restart app to refresh records.")
        } catch (e: Exception) {
            Pair(false, "Restore failed: ${e.localizedMessage}")
        }
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
