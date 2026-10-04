package com.loadtracker.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FacilityNotesRoleTest {

    @Test
    fun test1_FacilityRoleIdentification_IdentifiesShipperRoleCorrectly() {
        val note = FacilityNote(
            tripNotes = "Shipper check-in at Gate 3. Scale on site.",
            pickupTimestamp = 1700000000000L,
            proNumber = "52167364",
            shipperName = "SDI Sinton, TX",
            consigneeName = "Nucor Steel Memphis, TN"
        )

        val selectedFacility = "SDI Sinton, TX"
        val isShipper = note.shipperName.equals(selectedFacility, ignoreCase = true)

        assertTrue("Selected facility should be identified as Shipper", isShipper)
        assertEquals("SDI Sinton, TX", note.shipperName)
        assertEquals("Nucor Steel Memphis, TN", note.consigneeName)
    }

    @Test
    fun test2_FacilityRoleIdentification_IdentifiesReceiverRoleCorrectly() {
        val note = FacilityNote(
            tripNotes = "Receiver requires hard hat and steel-toe boots.",
            pickupTimestamp = 1700000000000L,
            proNumber = "52167364",
            shipperName = "SDI Sinton, TX",
            consigneeName = "Nucor Steel Memphis, TN"
        )

        val selectedFacility = "Nucor Steel Memphis, TN"
        val isReceiver = note.consigneeName.equals(selectedFacility, ignoreCase = true)

        assertTrue("Selected facility should be identified as Receiver", isReceiver)
    }
}
