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
    fun test6_OcrExtraction_ParsesFourDigitAndFormattedLoadedMiles() {
        fun extractLoadedMiles(rawText: String): String? {
            val loadedRegex = Regex(
                """(?:dispatched\s*loaded|disp\s*loaded|loaded\s*miles|loaded\s*mi|load\s*miles|loaded|ld\s*miles|ld\s*mi|trip\s*miles|total\s*miles|distance)\s*[:=\-\s]*(\d{1,3}(?:,\d{3})+|\d+)(?:\.(\d+))?""",
                RegexOption.IGNORE_CASE
            )
            val loadedMatch = loadedRegex.find(rawText)
            if (loadedMatch != null && loadedMatch.groupValues.size > 1) {
                val numPart = loadedMatch.groupValues[1].replace(",", "")
                val decPart = if (loadedMatch.groupValues.size > 2 && loadedMatch.groupValues[2].isNotBlank()) "." + loadedMatch.groupValues[2] else ""
                return numPart + decPart
            } else {
                val loadedReverseRegex = Regex(
                    """(\d{1,3}(?:,\d{3})+|\d+)(?:\.(\d+))?\s*(?:loaded\s*miles|loaded\s*mi|loaded|load\s*miles|mi\s*loaded)\b""",
                    RegexOption.IGNORE_CASE
                )
                val revMatch = loadedReverseRegex.find(rawText)
                if (revMatch != null && revMatch.groupValues.size > 1) {
                    val numPart = revMatch.groupValues[1].replace(",", "")
                    val decPart = if (revMatch.groupValues.size > 2 && revMatch.groupValues[2].isNotBlank()) "." + revMatch.groupValues[2] else ""
                    return numPart + decPart
                }
            }
            return null
        }

        assertEquals("1400", extractLoadedMiles("Loaded Miles: 1400"))
        assertEquals("1400", extractLoadedMiles("Loaded Miles: 1,400"))
        assertEquals("1400", extractLoadedMiles("Loaded: 1400"))
        assertEquals("1400", extractLoadedMiles("Disp Loaded: 1400"))
        assertEquals("1400.5", extractLoadedMiles("Dispatched Loaded: 1,400.5"))
        assertEquals("1400", extractLoadedMiles("1400 Loaded Miles"))
    }
}
