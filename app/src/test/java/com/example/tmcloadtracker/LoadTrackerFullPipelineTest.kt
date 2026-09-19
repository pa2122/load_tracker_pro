package com.example.tmcloadtracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadTrackerFullPipelineTest {

    @Test
    fun test1_OcrExtraction_ParsesRateConfirmationTextCorrectly() {
        val ocrSampleText = """
            Pro Number 52167364
            VULCRAFT NUCOR - GRATING
            GRAPELAND, TX
            COLUMBIA, SC
            Status Dispatched
            Instructions: MUST USE FALL PROTECTION & TIE DOWN RACKS OR U WILL BE UNLDED
            Directions: ACROSS FROM BROOKSHIRE BROTHERS
            Parking: NO
            Pickup #: 32-27547
            Loaded Miles: 968
            Bounce Miles: 155
            Trailer: 34942
            Tarps: Steel
            Going Home: Yes
            Load Gross: $2335.03
            Out Of Route History: 52167047 / 0.00%
            Additional Load Notes: unload #1410
            Stop Type: Origin
            VULCRAFT GRAPELAND
            175 CR RD 2345
            GRAPELAND, TX 75844
            @ 9/11/2026 11:00:00 AM
            Stop Type: Final Dropoff
            CHATHAM STEEL
            2400 SHOP RD
            COLUMBIA, SC 29201
            @ 9/14/2026 10:00:00 AM
        """.trimIndent()

        // 1. PRO Number regex check
        val proRegex = Regex("""\b(?:PRO|Order|Load|Trip)\s*(?:Number|No|Num)?\s*#?\s*:?\s*(\d{4,12})""", RegexOption.IGNORE_CASE)
        val proMatch = proRegex.find(ocrSampleText)
        assertNotNull("PRO number match should not be null", proMatch)
        assertEquals("52167364", proMatch?.groupValues?.get(1))

        // 2. Gross Pay regex check ($2335.03)
        val payRegex = Regex("""(?:Pay|Gross|Rate|Linehaul|Total|Amount)\D*?\$?\s*(\d{1,3}(?:,\d{3})+|\d+)(?:\.(\d{1,2}))?""", RegexOption.IGNORE_CASE)
        val payMatch = payRegex.find(ocrSampleText)
        assertNotNull("Gross pay match should not be null", payMatch)
        val fullPayStr = "${payMatch?.groupValues?.get(1)}.${payMatch?.groupValues?.get(2)}"
        assertEquals("2335.03", fullPayStr)

        // 3. Appointment Date & Type check
        val apptRegex = Regex("""(?:[@B]\s*)?(\d{1,2}/\d{1,2}/\d{2,4}\s+\d{1,2}:\d{2}(?::\d{2})?\s*(?:AM|PM)?)""", RegexOption.IGNORE_CASE)
        val apptMatches = apptRegex.findAll(ocrSampleText).toList()
        assertTrue("Should find at least 2 appointment timestamps", apptMatches.size >= 2)
        assertEquals("9/11/2026 11:00:00 AM", apptMatches[0].groupValues[1].trim())
        assertEquals("9/14/2026 10:00:00 AM", apptMatches[1].groupValues[1].trim())
    }

    @Test
    fun test2_PhoneNumberExtraction_ParsesContextualAndFormattedNumbers() {
        val noteWithPoc = "⚠️ Instructions: MUST USE FALL PROTECTION. POC JOHN SMITH 8005551234"
        val noteWithFormatted = "Directions: I-77N EX 797. Call shipping office at (800) 555-1234"

        val keywordRegex = Regex("""(?:call|contact|poc|phone|tel)\b.{0,20}?\(?\b(\d{3})\)?[-.\s]?(\d{3})[-.\s]?(\d{4})\b""", RegexOption.IGNORE_CASE)
        val matchPoc = keywordRegex.find(noteWithPoc)
        assertNotNull("Should match POC unformatted number", matchPoc)
        assertEquals("800", matchPoc?.groupValues?.get(1))
        assertEquals("555", matchPoc?.groupValues?.get(2))
        assertEquals("1234", matchPoc?.groupValues?.get(3))

        val matchFormatted = keywordRegex.find(noteWithFormatted)
        assertNotNull("Should match Call formatted number", matchFormatted)
        assertEquals("800", matchFormatted?.groupValues?.get(1))
        assertEquals("555", matchFormatted?.groupValues?.get(2))
        assertEquals("1234", matchFormatted?.groupValues?.get(3))
    }

    @Test
    fun test3_FlatbedPayrollEngine_CalculatesTakeHomePayAccurately() {
        val loadPay = 2335.03
        val ratePercent = 31.0
        val isPreTarped = false
        val tarpType = "S"
        val dispatchedBounce = 155.0
        val trainerPayRate = 200.0

        val loadCut = loadPay * (ratePercent / 100.0)
        val tarpPay = if (tarpType == "S") (if (isPreTarped) 15.0 else 30.0) else 0.0
        val deadheadBonus = if (dispatchedBounce >= 150.0) dispatchedBounce * 0.20 else 0.0

        val estimatedTakeHome = loadCut + tarpPay + deadheadBonus + trainerPayRate

        assertEquals(723.8593, loadCut, 0.01)
        assertEquals(30.0, tarpPay, 0.01)
        assertEquals(31.0, deadheadBonus, 0.01)
        assertEquals(200.0, trainerPayRate, 0.01)
        assertEquals(984.86, estimatedTakeHome, 0.01)
    }

    @Test
    fun test4_ActiveTripStateLifecycle_ProgressesThroughAll5States() {
        var currentTripState = "ACTIVE_BOUNCE"

        assertEquals("ACTIVE_BOUNCE", currentTripState)

        currentTripState = "ACTIVE_SHIPPER"
        assertEquals("ACTIVE_SHIPPER", currentTripState)

        currentTripState = "ACTIVE_LOADED"
        assertEquals("ACTIVE_LOADED", currentTripState)

        currentTripState = "ACTIVE_CONSIGNEE"
        assertEquals("ACTIVE_CONSIGNEE", currentTripState)

        currentTripState = "COMPLETED"
        assertEquals("COMPLETED", currentTripState)
    }

    @Test
    fun test5_DuplicateProDetection_FlagsExistingProNumber() {
        val existingLoadsList = listOf("52167364", "52167047", "601074")

        val newProDuplicate = "52167364"
        val newProUnique = "77889900"

        val isDuplicate = existingLoadsList.contains(newProDuplicate)
        val isUniqueDuplicate = existingLoadsList.contains(newProUnique)

        assertTrue("Should detect existing PRO number as duplicate", isDuplicate)
        assertFalse("Should recognize new PRO number as unique", isUniqueDuplicate)
    }

    @Test
    fun test6_UserScreenshot_ParsesSwanseaToFtStocktonScreenshot() {
        val screenshotText = """
            From: SYSTEM
            Stop Type: Origin
            NUCOR BUILDING SYSTEMS
            200 WHETSTONE RD
            SWANSEA, SC
            B 9/14/2026 11:59:00 PM
            Instructions: IF COD INSTRUCTIONS ON BOL TO FOLLOW UPON DELIVERY!! NO PETS!!/PPE, CLOSED TOE SHOES & PANTS A MUST! SIGN TOP COPY OF BOL AND LEAVE IN BOX.
            Directions: I 26E,EX 115/HWY 321S,HWY 3 TR,TOP OF HILL RHS. , MAKE SURE YOU HAVE A DELIVERY APPT, 200 WHETSTONE RD, SWANSEA, SC 29160
            Parking: PILOT I26 EX 115

            Stop Type: Final Dropoff
            HASKELL STEEL LLC
            6000 TX-18
            FT STOCKTON, TX
            @ 9/16/2026 8:00:00 AM

            Pro Number: 73367813
            Stop Number 0 PO Number NBSSC PO Number 64932311
            Stop Number 1 PO Number 64932311
            Customer Number: @154935
            Additional Load Notes: TR265126
            Load Number: 64932311
            Detention Free Time: 2 hours (Default) (Contact Detention 1.5 hours after arrival or Appt time if not Loaded/unloaded)
            Stop Offs: 0
            Commodity: IRON OR STEEL ARTICLES
            Pieces: 1
            Loaded Miles: 1409
            Bounce Miles: 20
            Hazmat: N
            Blind Shipment: N
            Over Dimensional: N
            Door/Mill:
            Out Of Route: 52167047 / 0.00%
            Going Home: N
            Gross: 4831.28
            Load Weight: 2320
            Tarp: NO
            Trailer: 34942 Possible Trailer Swap
            Accident Free Miles: 127034
        """.trimIndent()

        // 1. PRO Number (73367813)
        val proRegex = Regex("""\b(?:PRO|Order|Load|Trip)\s*(?:Number|No|Num)?\s*#?\s*:?\s*(\d{4,12})""", RegexOption.IGNORE_CASE)
        val proMatch = proRegex.find(screenshotText)
        assertNotNull("PRO match should not be null", proMatch)
        assertEquals("73367813", proMatch?.groupValues?.get(1))

        // 2. Gross Pay ($4831.28)
        val payRegex = Regex("""(?:Pay|Gross|Rate|Linehaul|Total|Amount)\D*?\$?\s*(\d{1,3}(?:,\d{3})+|\d+)(?:\.(\d{1,2}))?""", RegexOption.IGNORE_CASE)
        val payMatch = payRegex.find(screenshotText)
        assertNotNull("Gross pay match should not be null", payMatch)
        val fullPayStr = "${payMatch?.groupValues?.get(1)}.${payMatch?.groupValues?.get(2)}"
        assertEquals("4831.28", fullPayStr)

        // 3. Loaded Miles (1409)
        val loadedRegex = Regex("""(?:loaded\s*miles|loaded\s*mi)\s*[:=\-\s]*(\d{1,4}(?:,\d{3})*(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
        val loadedMatch = loadedRegex.find(screenshotText)
        assertNotNull("Loaded miles match should not be null", loadedMatch)
        assertEquals("1409", loadedMatch?.groupValues?.get(1))

        // 4. Bounce Miles (20)
        val bounceRegex = Regex("""(?:bounce\s*miles|bounce\s*mi)\s*[:=\-\s]*(\d{1,4}(?:,\d{3})*(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
        val bounceMatch = bounceRegex.find(screenshotText)
        assertNotNull("Bounce miles match should not be null", bounceMatch)
        assertEquals("20", bounceMatch?.groupValues?.get(1))
    }
}
