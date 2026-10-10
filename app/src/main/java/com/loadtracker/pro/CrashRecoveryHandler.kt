package com.loadtracker.pro

import android.content.Context
import android.util.Log

object CrashRecoveryHandler {

    private const val PREFS_NAME = "crash_recovery_prefs"
    private const val KEY_WAS_RECOVERED = "was_crash_recovered"
    private const val KEY_SAVED_PRO = "saved_pro_number"
    private const val KEY_SAVED_SEGMENT = "saved_active_segment"
    private const val KEY_CRASH_TIMESTAMP = "crash_timestamp"

    data class RecoveredCrashState(
        val wasRecovered: Boolean,
        val proNumber: String?,
        val activeSegment: String?,
        val timestamp: Long
    )

    /**
     * Registers global uncaught exception handler to save active trip state to disk before process death.
     */
    fun setupGlobalExceptionHandler(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e("CRASH_RECOVERY", "Intercepted uncaught crash in thread ${thread.name}", throwable)
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

                val activePro = TrackingService.activeProNumber
                val activeSegment = TrackingService.activeSegment

                prefs.edit().apply {
                    putBoolean(KEY_WAS_RECOVERED, true)
                    putString(KEY_SAVED_PRO, activePro)
                    putString(KEY_SAVED_SEGMENT, activeSegment)
                    putLong(KEY_CRASH_TIMESTAMP, System.currentTimeMillis())
                    apply()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    /**
     * Checks if the app was restarted after an unexpected crash.
     */
    fun checkAndClearCrashRecoveryState(context: Context): RecoveredCrashState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val wasRecovered = prefs.getBoolean(KEY_WAS_RECOVERED, false)
        val proNumber = prefs.getString(KEY_SAVED_PRO, null)
        val segment = prefs.getString(KEY_SAVED_SEGMENT, null)
        val timestamp = prefs.getLong(KEY_CRASH_TIMESTAMP, 0L)

        if (wasRecovered) {
            prefs.edit().clear().apply()
        }

        return RecoveredCrashState(
            wasRecovered = wasRecovered,
            proNumber = proNumber,
            activeSegment = segment,
            timestamp = timestamp
        )
    }
}
