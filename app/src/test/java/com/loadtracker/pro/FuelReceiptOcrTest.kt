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

    @Test
    fun test4_FuelReceiptOcr_ParsesArdmoreScreenshot() {
        val rawText = """
            Store 701
            2450 Cooper Dr
            Ardmore, OK 73401
            (580) 226-3833
            10/08/2026
            
            Receipt: 99265702
            Type: Sale
            
            ========================================
            Qty Name                            Price
            1 TrDsl                           $6.199 $
            Pump: 19
            Gallons: 116.4
            Price/Gal: $0.000
            
            1 DEF                             $4.899
            Pump: 19
            Gallons: 10.951
            Price/Gal: $0.000
            
            ----------------------------------------
            Subtotal                               $
            Sales Tax
            ----------------------------------------
            Total                                  $
            ========================================
            Amount Tendered                        $
            TCH
            XXXXXXXXXXXX7662
            Authorization: 358299
            Truck Company: TMC TRANSPORTATION
            
            ----------------------------------------
            Total Tendered                         $
            ========================================
            Loyalty Card:           XXXXXXXXX1827
            Vehicle ID:             XXXXX
            Odometer:               244322
            AdditionalProducts: N
        """.trimIndent()
        
        val parsed = FuelReceiptOcrParser.parseReceiptText(rawText)
        assertEquals(116.4, parsed.gallons!!, 0.001)
        assertEquals(6.199, parsed.pricePerGallon!!, 0.001)
        assertEquals(10.951, parsed.defGallons!!, 0.001)
        assertEquals(53.65, parsed.defCost!!, 0.01)
        assertEquals(244322.0, parsed.odometer!!, 0.1)
        assertEquals("OK", parsed.state)
        assertEquals("10/08/2026", parsed.dateText)
        
        // Ensure the fallback cost calculation handles the $0.000 total block gracefully
        assertEquals(775.21, parsed.totalCost!!, 0.05)
    }

    @Test
    fun test5_FuelReceiptOcr_ParsesCouncilBluffsScreenshot() {
        val rawText = """
            Store 329
            2647 S 24th St
            Council Bluffs, IA 51501
            (712) 322-0088
            10/07/2026
            
            Receipt: 1124291
            Type: Sale
            
            ========================================
            Qty Name                            Price
            1 Truck Diesel                    $6.299 $
            Pump: 20
            Gallons: 144.747
            Price/Gal: $6.299
            
            1 Cash Advance Item               $25.00
            1 Cash Adv Fee                    $0.00
            
            ----------------------------------------
            Subtotal                               $
            Sales Tax
            ----------------------------------------
            Total                                  $
            ========================================
            Amount Tendered                        $
            TCH Card
            XXXXXXXXXXXX7662
            Authorization: 354295
            Auth. Msg.: Approved
            Truck Company: TMC TRANSPORTATION
            Invoice Number: 0860007205
            
            Vehicle ID:             XXXXX
            Odometer:               243448
        """.trimIndent()
        
        val parsed = FuelReceiptOcrParser.parseReceiptText(rawText)
        assertEquals(144.747, parsed.gallons!!, 0.001)
        assertEquals(6.299, parsed.pricePerGallon!!, 0.001)
        assertEquals(243448.0, parsed.odometer!!, 0.1)
        assertEquals("IA", parsed.state)
        assertEquals("10/07/2026", parsed.dateText)
    }
}
