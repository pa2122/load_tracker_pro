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

    @Test
    fun test3_FuelReceiptOcr_ParsesPilotFlyingJDigitalReceiptScreenshot() {
        val pilotDigitalReceipt = """
            Store 1057
            1305 Pasadena Fwy
            Pasadena, TX 77506
            (713) 534-0038
            10/05/2026
            
            Qty Name
            1 Truck Diesel
            Pump: 21
            Gallons: 123.597
            Price/Gal: $5.859
            
            1 DEF Fuel Item
            Pump: 21
            Gallons: 10.609
            Price/Gal: $4.899
            
            Vehicle ID: XXXXX
            Odometer: 242481
        """.trimIndent()

        val parsed = FuelReceiptOcrParser.parseReceiptText(pilotDigitalReceipt)

        assertNotNull("Parsed data should not be null", parsed)
        assertEquals(123.597, parsed.gallons!!, 0.001)
        assertEquals(5.859, parsed.pricePerGallon!!, 0.001)
        assertEquals(10.609, parsed.defGallons!!, 0.001)
        assertEquals(51.97, parsed.defCost!!, 0.01)
        assertEquals(242481.0, parsed.odometer!!, 0.1)
        assertEquals("TX", parsed.state)
    }
}
