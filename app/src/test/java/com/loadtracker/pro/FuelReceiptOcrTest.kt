package com.loadtracker.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class FuelReceiptOcrTest {

    @Test
    fun test1_FuelReceiptOcr_ParsesLovesReceiptTextCorrectly() {
        val sampleLovesReceipt = """
            LOVE'S TRAVEL STOPS #410
            I-35 EXIT 88
            STORE #00410
            STATION # 02
            
            DIESEL #2
            GALLONS: 80.000
            PPG: $3.500
            
            TOTAL AMOUNT: $280.00
            CARD: XXXXXXXXXXXX1234
            THANK YOU FOR PUMPING AT LOVE'S!
        """.trimIndent()

        val parsed = FuelReceiptOcrParser.parseReceiptText(sampleLovesReceipt)

        assertNotNull("Parsed data should not be null", parsed)
        assertEquals("Love's", parsed.stationName)
        assertEquals(80.00, parsed.gallons!!, 0.01)
        assertEquals(3.500, parsed.pricePerGallon!!, 0.001)
        assertEquals(280.00, parsed.totalCost!!, 0.01)
    }

    @Test
    fun test2_FuelReceiptOcr_ParsesPilotReceiptTextCorrectly() {
        val samplePilotReceipt = """
            PILOT FLYING J #205
            HOPE, AR
            
            ULSD DIESEL
            QTY: 100.500 GAL
            PRICE/GAL: $3.450
            
            NET TOTAL: $346.73
            APPROVED
        """.trimIndent()

        val parsed = FuelReceiptOcrParser.parseReceiptText(samplePilotReceipt)

        assertNotNull("Parsed data should not be null", parsed)
        assertEquals("Pilot Flying J", parsed.stationName)
        assertEquals(100.50, parsed.gallons!!, 0.01)
        assertEquals(3.450, parsed.pricePerGallon!!, 0.001)
        assertEquals(346.73, parsed.totalCost!!, 0.01)
    }
}
