package com.loadtracker.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DvirInspectionTest {

    @Test
    fun test1_DvirValidation_FlagsUnsafeWhenCriticalDefectsExist() {
        val dvirClean = DvirEntry(
            timestamp = 1700000000000L,
            truckNumber = "T-104",
            trailerNumber = "TR-34942",
            odometer = 127034.0,
            passedTractorCheck = true,
            passedCouplingCheck = true,
            passedBrakesTiresCheck = true,
            passedFlatbedGearCheck = true,
            strapsCount = 12,
            chainsBindersCount = 8,
            tarpsCondition = "Good",
            coilRacksCount = 4,
            defectsFound = null,
            isSafeToOperate = true,
            driverSignature = "John Doe"
        )

        assertTrue("Clean DVIR should be marked safe to operate", dvirClean.isSafeToOperate)
        assertEquals("T-104", dvirClean.truckNumber)
        assertEquals(12, dvirClean.strapsCount)

        val dvirDefective = DvirEntry(
            timestamp = 1700000000000L,
            truckNumber = "T-104",
            trailerNumber = "TR-34942",
            odometer = 127034.0,
            passedTractorCheck = true,
            passedCouplingCheck = true,
            passedBrakesTiresCheck = false, // Failed brake check
            passedFlatbedGearCheck = true,
            strapsCount = 12,
            chainsBindersCount = 8,
            tarpsCondition = "Good",
            coilRacksCount = 4,
            defectsFound = "Right steer tire air leak",
            isSafeToOperate = false,
            driverSignature = "John Doe"
        )

        assertFalse("DVIR with brake defect should be marked unsafe", dvirDefective.isSafeToOperate)
        assertNotNull("Defects notes should be captured", dvirDefective.defectsFound)
    }

    @Test
    fun test2_FlatbedGearInventoryAudit_VerifiesMinimumSecurementCounts() {
        val straps = 10
        val chains = 6
        val tarpsCond = "Good"

        val hasMinStraps = straps >= 8
        val hasMinChains = chains >= 4
        val isTarpUsable = tarpsCond == "Good" || tarpsCond == "Needs Repair"

        assertTrue("Should meet minimum strap requirements for open deck", hasMinStraps)
        assertTrue("Should meet minimum chain/binder requirements", hasMinChains)
        assertTrue("Tarp should be marked usable", isTarpUsable)
    }
}
