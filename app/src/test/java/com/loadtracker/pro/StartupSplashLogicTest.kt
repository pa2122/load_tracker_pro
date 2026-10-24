package com.loadtracker.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupSplashLogicTest {

    private fun step(
        settingsRead: Boolean = true,
        loadsLoaded: Boolean = true,
        unfinished: Boolean = false,
        restored: Boolean = false,
        recovered: Boolean = false
    ) = StartupSplashLogic.currentStep(settingsRead, loadsLoaded, unfinished, restored, recovered)

    private fun show(
        settingsRead: Boolean = true,
        loadsLoaded: Boolean = true,
        unfinished: Boolean = false,
        restored: Boolean = false,
        dismissed: Boolean = false
    ) = StartupSplashLogic.shouldShowSplash(settingsRead, loadsLoaded, unfinished, restored, dismissed)

    @Test
    fun stepsFollowRealStartupOrder() {
        assertEquals(StartupStep.READING_SETTINGS, step(settingsRead = false, loadsLoaded = false))
        assertEquals(StartupStep.OPENING_DATABASE, step(loadsLoaded = false))
    }

    @Test
    fun restoringStepOnlyWhenUnfinishedLoadExists() {
        assertEquals(StartupStep.RESTORING_ACTIVE_LOAD, step(unfinished = true))
        assertEquals(StartupStep.OPENING_DATABASE, step(unfinished = false))
    }

    @Test
    fun recoveredStepOnlyWhenCrashRecoveryFired() {
        assertEquals(StartupStep.RECOVERED_AFTER_SHUTDOWN, step(unfinished = true, recovered = true))
        assertEquals(StartupStep.OPENING_DATABASE, step(unfinished = false, recovered = true))
    }

    @Test
    fun splashStaysUntilSettingsAndFirstLoadsEmission() {
        assertTrue(show(settingsRead = false, loadsLoaded = true))
        assertTrue(show(loadsLoaded = false))
        assertFalse(show())
    }

    @Test
    fun splashWaitsForActiveLoadRestore() {
        assertTrue(show(unfinished = true, restored = false))
        assertFalse(show(unfinished = true, restored = true))
    }

    @Test
    fun splashIsSkippedOnceDismissedEvenIfStateResets() {
        assertFalse(show(settingsRead = false, loadsLoaded = false, dismissed = true))
    }

    @Test
    fun timeoutTriggersAtLimit() {
        assertFalse(StartupSplashLogic.hasTimedOut(StartupSplashLogic.TIMEOUT_MS - 1))
        assertTrue(StartupSplashLogic.hasTimedOut(StartupSplashLogic.TIMEOUT_MS))
    }

    @Test
    fun unfinishedLoadStates() {
        assertTrue(StartupSplashLogic.isUnfinished("ACTIVE_LOADED"))
        assertFalse(StartupSplashLogic.isUnfinished("COMPLETED"))
        assertFalse(StartupSplashLogic.isUnfinished("NOT_STARTED"))
        assertFalse(StartupSplashLogic.isUnfinished("PAUSED_AT_HOME"))
    }
}
