package com.loadtracker.pro

import java.util.Locale

object FuelReceiptOcrParser {

    data class ParsedReceiptData(
        val totalCost: Double? = null,
        val gallons: Double? = null,
        val pricePerGallon: Double? = null,
        val stationName: String? = null,
        val defGallons: Double? = null,
        val defCost: Double? = null,
        val odometer: Double? = null,
        val state: String? = null
    )

    /**
     * Parses raw OCR text from a fuel receipt photo or screenshot.
     */
    fun parseReceiptText(rawText: String): ParsedReceiptData {
        if (rawText.isBlank()) return ParsedReceiptData()

        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

        var totalCost: Double? = null
        var gallons: Double? = null
        var pricePerGallon: Double? = null
        var stationName: String? = null
        var defGallons: Double? = null
        var defCost: Double? = null
        var odometer: Double? = null
        var state: String? = null

        // 1. Station Name Detection
        for (line in lines.take(10)) {
            val lower = line.lowercase(Locale.US)
            when {
                lower.contains("love") -> { stationName = "Love's"; break }
                lower.contains("pilot") || lower.contains("flying j") -> { stationName = "Pilot Flying J"; break }
                lower.contains("ta ") || lower.contains("petro") || lower.contains("travelcenters") -> { stationName = "TA / Petro"; break }
                lower.contains("speedway") -> { stationName = "Speedway"; break }
                lower.contains("kwik trip") || lower.contains("kwik star") -> { stationName = "Kwik Trip"; break }
            }
        }

        // 2. Diesel Gallons Extraction
        val gallonsRegex = Regex("""(?:truck\s*diesel|diesel|gallons|gal|qty|volume|v)\D*?(\d{1,3}\.\d{1,3})""", RegexOption.IGNORE_CASE)
        val gallonsMatch = gallonsRegex.find(rawText)
        if (gallonsMatch != null && gallonsMatch.groupValues.size > 1) {
            gallons = gallonsMatch.groupValues[1].toDoubleOrNull()
        }

        // 3. Diesel Price Per Gallon (PPG) Extraction
        val ppgRegex = Regex("""(?:ppg|price/gal|\$/g|rate|price)\D*?\$?\s*(\d\.\d{3})""", RegexOption.IGNORE_CASE)
        val ppgMatch = ppgRegex.find(rawText)
        if (ppgMatch != null && ppgMatch.groupValues.size > 1) {
            pricePerGallon = ppgMatch.groupValues[1].toDoubleOrNull()
        }

        // 4. DEF Gallons & DEF Cost Extraction
        val defSectionRegex = Regex("""DEF\b[\s\S]*?(?:Gallons|Gal|Qty)\D*?(\d{1,3}\.\d{1,3})[\s\S]*?(?:Price/Gal|PPG|\$/G)\D*?\$?\s*(\d\.\d{3})""", RegexOption.IGNORE_CASE)
        val defMatch = defSectionRegex.find(rawText)
        if (defMatch != null && defMatch.groupValues.size > 2) {
            defGallons = defMatch.groupValues[1].toDoubleOrNull()
            val defPpg = defMatch.groupValues[2].toDoubleOrNull()
            if (defGallons != null && defPpg != null) {
                defCost = defGallons * defPpg
            }
        }

        // 5. Odometer Extraction
        val odoRegex = Regex("""(?:odometer|odo|mileage)\D*?(\d{5,6})""", RegexOption.IGNORE_CASE)
        val odoMatch = odoRegex.find(rawText)
        if (odoMatch != null && odoMatch.groupValues.size > 1) {
            odometer = odoMatch.groupValues[1].toDoubleOrNull()
        }

        // 6. State Extraction
        val stateRegex = Regex("""\b([A-Z]{2})\s+\d{5}\b""")
        val stateMatch = stateRegex.find(rawText)
        if (stateMatch != null && stateMatch.groupValues.size > 1) {
            val candidateState = stateMatch.groupValues[1].uppercase(Locale.US)
            if (candidateState in US_STATE_CODES) {
                state = candidateState
            }
        }

        // 7. Total Cost Extraction
        val totalRegex = Regex("""(?:total|amount|net\s*total|net|paid)\D*?\$?\s*(\d{1,4}\.\d{2})""", RegexOption.IGNORE_CASE)
        val totalMatch = totalRegex.find(rawText)
        if (totalMatch != null && totalMatch.groupValues.size > 1) {
            totalCost = totalMatch.groupValues[1].toDoubleOrNull()
        } else {
            // Fallback: calculate totalCost from gallons * ppg if available
            if (gallons != null && pricePerGallon != null) {
                totalCost = gallons * pricePerGallon
            }
        }

        return ParsedReceiptData(
            totalCost = totalCost,
            gallons = gallons,
            pricePerGallon = pricePerGallon,
            stationName = stationName,
            defGallons = defGallons,
            defCost = defCost,
            odometer = odometer,
            state = state
        )
    }

    private val US_STATE_CODES = setOf(
        "AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "FL", "GA", "HI", "ID", "IL", "IN", "IA", "KS",
        "KY", "LA", "ME", "MD", "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ", "NM", "NY",
        "NC", "ND", "OH", "OK", "OR", "PA", "RI", "SC", "SD", "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY"
    )
}
