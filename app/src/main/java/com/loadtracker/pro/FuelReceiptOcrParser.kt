package com.loadtracker.pro

import java.util.Locale

object FuelReceiptOcrParser {

    data class ParsedReceiptData(
        val totalCost: Double? = null,
        val gallons: Double? = null,
        val pricePerGallon: Double? = null,
        val stationName: String? = null
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

        // 1. Station Name Detection
        for (line in lines.take(5)) {
            val lower = line.lowercase(Locale.US)
            when {
                lower.contains("love") -> { stationName = "Love's"; break }
                lower.contains("pilot") || lower.contains("flying j") -> { stationName = "Pilot Flying J"; break }
                lower.contains("ta ") || lower.contains("petro") || lower.contains("travelcenters") -> { stationName = "TA / Petro"; break }
                lower.contains("speedway") -> { stationName = "Speedway"; break }
                lower.contains("kwik trip") || lower.contains("kwik star") -> { stationName = "Kwik Trip"; break }
            }
        }

        // 2. Gallons Extraction
        val gallonsRegex = Regex("""(?:gallons|gal|qty|volume|v)\D*?(\d{1,3}\.\d{1,3})""", RegexOption.IGNORE_CASE)
        val gallonsMatch = gallonsRegex.find(rawText)
        if (gallonsMatch != null && gallonsMatch.groupValues.size > 1) {
            gallons = gallonsMatch.groupValues[1].toDoubleOrNull()
        }

        // 3. Price Per Gallon (PPG) Extraction
        val ppgRegex = Regex("""(?:ppg|price/gal|\$/g|rate|price)\D*?\$?\s*(\d\.\d{3})""", RegexOption.IGNORE_CASE)
        val ppgMatch = ppgRegex.find(rawText)
        if (ppgMatch != null && ppgMatch.groupValues.size > 1) {
            pricePerGallon = ppgMatch.groupValues[1].toDoubleOrNull()
        }

        // 4. Total Cost Extraction
        val totalRegex = Regex("""(?:total|amount|net\s*total|net|paid)\D*?\$?\s*(\d{1,4}\.\d{2})""", RegexOption.IGNORE_CASE)
        val totalMatch = totalRegex.find(rawText)
        if (totalMatch != null && totalMatch.groupValues.size > 1) {
            totalCost = totalMatch.groupValues[1].toDoubleOrNull()
        } else {
            // Fallback: match largest currency pattern
            val currencyMatches = Regex("""\$?\s*(\d{2,4}\.\d{2})""").findAll(rawText).mapNotNull { it.groupValues[1].toDoubleOrNull() }.toList()
            if (currencyMatches.isNotEmpty()) {
                totalCost = currencyMatches.maxOrNull()
            }
        }

        return ParsedReceiptData(
            totalCost = totalCost,
            gallons = gallons,
            pricePerGallon = pricePerGallon,
            stationName = stationName
        )
    }
}
