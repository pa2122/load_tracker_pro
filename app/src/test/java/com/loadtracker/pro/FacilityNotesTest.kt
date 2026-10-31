package com.loadtracker.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FacilityNotesTest {
    private val blob = """
        --- SHIPPER FACILITY NOTES ---
        ⚠️ Instructions: PRLDS MUST LEAVE 2 GOODL TRPS,10 STRAPS ON DECK OF MT TRLR
        🗺️ Directions: | 44W,EX @62,N2700 RD TR
        🅿️ Parking: Y, BUT NO PRK IN TARP

        --- CONSIGNEE FACILITY NOTES ---
        🗺️ Directions: I-35 S, EX 265, CONT TX-130 S
        🅿️ Parking: YES
    """.trimIndent()

    @Test
    fun splitsShipperAndConsigneeSections() {
        val (shipper, consignee) = FacilityNotes.parseTripNotes(blob)
        assertEquals("PRLDS MUST LEAVE 2 GOODL TRPS,10 STRAPS ON DECK OF MT TRLR", shipper.instructions)
        assertEquals("| 44W,EX @62,N2700 RD TR", shipper.directions)
        assertEquals("Y, BUT NO PRK IN TARP", shipper.parking)
        assertEquals("", consignee.instructions)
        assertEquals("I-35 S, EX 265, CONT TX-130 S", consignee.directions)
        assertEquals("YES", consignee.parking)
    }

    @Test
    fun notesWithoutHeadersAreIgnored() {
        val (shipper, consignee) = FacilityNotes.parseTripNotes("Gate code 1234, call ahead")
        assertTrue(shipper.isEmpty)
        assertTrue(consignee.isEmpty)
    }

    @Test
    fun continuationLinesJoinTheCurrentField() {
        val (shipper, _) = FacilityNotes.parseTripNotes(
            "--- SHIPPER FACILITY NOTES ---\nInstructions: Check in at gate\nthen weigh out"
        )
        assertEquals("Check in at gate then weigh out", shipper.instructions)
    }

    @Test
    fun keySeparatesSameNameAtDifferentAddresses() {
        val a = FacilityNotes.key("CMC STEEL - TEXAS\n1 STEEL MILL DR\nSEGUIN, TX")
        val b = FacilityNotes.key("CMC STEEL - TEXAS\n200 MAIN ST\nWACO, TX")
        assertTrue(a != b)
    }

    @Test
    fun keyIgnoresCaseSpacingAndPunctuation() {
        assertEquals(
            FacilityNotes.key("Action Gypsum\n955 County Rd 118\nHutto, TX"),
            FacilityNotes.key("ACTION GYPSUM  955 COUNTY RD 118 HUTTO TX")
        )
    }

    @Test
    fun scannedNotesRoundTripThroughEncoding() {
        val scanned = ScannedFacilityNotes("key", FacilityNoteFields("i", "d", "p"))
        assertEquals(scanned, ScannedFacilityNotes.decode(scanned.encode()))
        assertNull(ScannedFacilityNotes.decode(""))
    }

    @Test
    fun newFacilityIsSavedSilently() {
        val scanned = FacilityNoteFields(directions = "I-35 S")
        assertEquals(FacilityNotePlan.Save(scanned), FacilityNotes.plan(null, scanned))
    }

    @Test
    fun emptyScanNeverChangesAnything() {
        assertEquals(FacilityNotePlan.NoChange, FacilityNotes.plan(FacilityNoteFields("a"), FacilityNoteFields()))
    }

    @Test
    fun ocrNoiseDoesNotTriggerPrompt() {
        val saved = FacilityNoteFields(instructions = "CHECK IN W/GUARD, LIVE LOAD CHECK @ KIOSK")
        val scanned = FacilityNoteFields(instructions = "CHECKIN W/GUARD, LIVE LOAD CHECK (@ KIOSK.")
        assertEquals(FacilityNotePlan.NoChange, FacilityNotes.plan(saved, scanned))
    }

    @Test
    fun blankSavedFieldsAreFilledWithoutAsking() {
        val saved = FacilityNoteFields(directions = "I-35 S")
        val scanned = FacilityNoteFields(directions = "I-35 S", parking = "YES")
        assertEquals(
            FacilityNotePlan.Save(FacilityNoteFields(directions = "I-35 S", parking = "YES")),
            FacilityNotes.plan(saved, scanned)
        )
    }

    @Test
    fun differingFieldAsksAndKeepsBothOptions() {
        val saved = FacilityNoteFields(instructions = "Old", parking = "NO")
        val scanned = FacilityNoteFields(instructions = "New", directions = "Turn left")
        val plan = FacilityNotes.plan(saved, scanned) as FacilityNotePlan.Ask
        assertEquals(FacilityNoteFields("Old", "Turn left", "NO"), plan.keepSaved)
        assertEquals(FacilityNoteFields("New", "Turn left", "NO"), plan.useScanned)
    }
}
