package com.loadtracker.pro

enum class StartupStep {
    READING_SETTINGS,
    OPENING_DATABASE,
    RESTORING_ACTIVE_LOAD,
    RECOVERED_AFTER_SHUTDOWN
}

object StartupSplashLogic {
    const val TIMEOUT_MS = 8_000L

    fun isUnfinished(tripState: String): Boolean =
        tripState != "COMPLETED" &&
            tripState != "NOT_STARTED" &&
            tripState != "PAUSED_AT_HOME"

    fun currentStep(
        settingsRead: Boolean,
        loadsLoaded: Boolean,
        hasUnfinishedLoad: Boolean,
        activeLoadRestored: Boolean,
        recoveredAfterShutdown: Boolean
    ): StartupStep = when {
        !settingsRead -> StartupStep.READING_SETTINGS
        !loadsLoaded -> StartupStep.OPENING_DATABASE
        hasUnfinishedLoad && !activeLoadRestored ->
            if (recoveredAfterShutdown) StartupStep.RECOVERED_AFTER_SHUTDOWN
            else StartupStep.RESTORING_ACTIVE_LOAD
        else -> StartupStep.OPENING_DATABASE
    }

    // The splash is only shown until startup finishes once; a recreated activity keeps the flag.
    fun shouldShowSplash(
        settingsRead: Boolean,
        loadsLoaded: Boolean,
        hasUnfinishedLoad: Boolean,
        activeLoadRestored: Boolean,
        alreadyDismissed: Boolean
    ): Boolean {
        if (alreadyDismissed) return false
        val restoreDone = !hasUnfinishedLoad || activeLoadRestored
        return !(settingsRead && loadsLoaded && restoreDone)
    }

    fun hasTimedOut(elapsedMs: Long): Boolean = elapsedMs >= TIMEOUT_MS
}
