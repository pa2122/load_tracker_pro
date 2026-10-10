package com.loadtracker.pro

import java.util.Locale
import kotlin.math.round

object FuelReceiptOcrParser {

    data class ParsedReceiptData(
        val totalCost: Double? = null,
        val gallons: Double? = null,
        val pricePerGallon: Double? = null,
        val stationName: String? = null,
        val defGallons: Double? = null,
        val defCost: Double? = null,
        val odometer: Double? = null,
        val state: String? = null,
        val dateText: String? = null
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
        var defPpg: Double? = null
        var odometer: Double? = null
        var state: String? = null
        var dateText: String? = null

        var inDieselBlock = false
        var inDefBlock = false

        // 1. Station Name Detection
        val lowerRawText = rawText.lowercase(Locale.US)
        when {
            lowerRawText.contains("love") -> stationName = "Love's"
            lowerRawText.contains("pilot") || lowerRawText.contains("flying j") -> stationName = "Pilot Flying J"
            lowerRawText.contains("ta ") || lowerRawText.contains("petro") || lowerRawText.contains("travelcenters") -> stationName = "TA / Petro"
            lowerRawText.contains("speedway") -> stationName = "Speedway"
            lowerRawText.contains("kwik trip") || lowerRawText.contains("kwik star") -> stationName = "Kwik Trip"
        }

        // Line by Line processing for blocks and scattered data
        for (i in lines.indices) {
            val line = lines[i]
            val lowerLine = line.lowercase(Locale.US)

            // State Extraction
            val stateRegex = Regex("""\b([A-Z]{2})\s+\d{5}\b""")
            val stateMatch = stateRegex.find(line)
            if (stateMatch != null && state == null) {
                val candidateState = stateMatch.groupValues[1].uppercase(Locale.US)
                if (candidateState in US_STATE_CODES) {
                    state = candidateState
                }
            }

            // Date Extraction
            val dateRegex = Regex("""\b(\d{1,2}[/-]\d{1,2}[/-]\d{2,4})\b""")
            val dateMatch = dateRegex.find(line)
            if (dateMatch != null && dateText == null) {
                dateText = dateMatch.groupValues[1]
            }

            // Odometer Extraction
            val odoRegex = Regex("""(?:odometer|odo|mileage)\D*?(\d{5,6})""", RegexOption.IGNORE_CASE)
            val odoMatch = odoRegex.find(line)
            if (odoMatch != null && odometer == null) {
                odometer = odoMatch.groupValues[1].toDoubleOrNull()
            } else if ((lowerLine == "odometer:" || lowerLine == "odometer" || lowerLine == "odo:") && odometer == null) {
                // If it's isolated on a line, check the next line
                if (i + 1 < lines.size) {
                    val nextLine = lines[i+1].trim()
                    val nextOdoMatch = Regex("""^(\d{5,6})$""").find(nextLine)
                    if (nextOdoMatch != null) {
                        odometer = nextOdoMatch.groupValues[1].toDoubleOrNull()
                    }
                }
            }

            // Total Cost Extraction
            if (lowerLine.contains("total") || lowerLine.contains("amount") || lowerLine.contains("net") || lowerLine.contains("paid") || lowerLine.contains("tendered")) {
                val totalRegex = Regex("""(?:total|amount|net\s*total|net|paid|tendered)\D*?\$?\s*(\d{1,4}\.\d{2})\b""", RegexOption.IGNORE_CASE)
                val totalMatch = totalRegex.find(line)
                if (totalMatch != null && totalCost == null) {
                    val possibleTotal = totalMatch.groupValues[1].toDoubleOrNull()
                    if (possibleTotal != null && possibleTotal > 0) {
                        totalCost = possibleTotal
                    }
                }
            }

            // Block transition detection
            val isDieselLine = lowerLine.matches(".*\\b(trdsl|truck diesel|diesel|ulsd)\\b.*".toRegex()) && !lowerLine.contains("evidence")
            val isDefLine = lowerLine.matches(".*\\b(def)\\b.*".toRegex()) && !lowerLine.contains("def points")

            if (isDieselLine) {
                inDieselBlock = true
                inDefBlock = false
                val priceMatch = Regex("""\$?\s*(\d+\.\d{3})\b""").findAll(line).toList()
                if (priceMatch.isNotEmpty()) {
                    pricePerGallon = priceMatch.last().groupValues[1].toDoubleOrNull()
                }
            } else if (isDefLine) {
                inDefBlock = true
                inDieselBlock = false
                val priceMatch = Regex("""\$?\s*(\d+\.\d{3})\b""").findAll(line).toList()
                if (priceMatch.isNotEmpty()) {
                    defPpg = priceMatch.last().groupValues[1].toDoubleOrNull()
                }
            }

            // Parse inside blocks
            if (inDieselBlock) {
                val galMatch = Regex("""(?:gallons|qty|volume)\D*?(\d{1,3}\.\d{1,3})""", RegexOption.IGNORE_CASE).find(line)
                if (galMatch != null && gallons == null) {
                    gallons = galMatch.groupValues[1].toDoubleOrNull()
                }

                val ppgMatch = Regex("""(?:price/gal|ppg|\$/g)\D*?\$?\s*(\d+\.\d{3})""", RegexOption.IGNORE_CASE).find(line)
                if (ppgMatch != null) {
                    val ppg = ppgMatch.groupValues[1].toDoubleOrNull()
                    if (ppg != null && ppg > 0.0 && pricePerGallon == null) {
                        pricePerGallon = ppg
                    }
                }
            }

            if (inDefBlock) {
                val galMatch = Regex("""(?:gallons|qty|volume)\D*?(\d{1,3}\.\d{1,3})""", RegexOption.IGNORE_CASE).find(line)
                if (galMatch != null && defGallons == null) {
                    defGallons = galMatch.groupValues[1].toDoubleOrNull()
                }

                val ppgMatch = Regex("""(?:price/gal|ppg|\$/g)\D*?\$?\s*(\d+\.\d{3})""", RegexOption.IGNORE_CASE).find(line)
                if (ppgMatch != null) {
                    val ppg = ppgMatch.groupValues[1].toDoubleOrNull()
                    if (ppg != null && ppg > 0.0 && defPpg == null) {
                        defPpg = ppg
                    }
                }
            }
        }

        // Calculate DEF cost (Safe variables to avoid smart cast issues)
        val finalDefGallons = defGallons
        val finalDefPpg = defPpg
        if (finalDefGallons != null && finalDefPpg != null) {
            val rawDefCost = finalDefGallons * finalDefPpg
            defCost = round(rawDefCost * 100.0) / 100.0
        }
        
        // Reverse-Calculate Diesel Price Per Gallon if missing/0.000
        val tempTotal = totalCost
        val tempGals = gallons
        if ((pricePerGallon == null || pricePerGallon == 0.0) && tempTotal != null && tempGals != null && tempGals > 0.0) {
            val defDeduction = defCost ?: 0.0
            val dieselOnlyCost = tempTotal - defDeduction
            if (dieselOnlyCost > 0.0) {
                 val calculatedPpg = dieselOnlyCost / tempGals
                 pricePerGallon = round(calculatedPpg * 1000.0) / 1000.0
            }
        }

        // Fallback for totalCost if not found
        val finalGallons = gallons
        val finalPpg = pricePerGallon
        if (totalCost == null && finalGallons != null && finalPpg != null) {
            var calculatedTotal = finalGallons * finalPpg
            if (defCost != null) {
                calculatedTotal += defCost
            }
            totalCost = round(calculatedTotal * 100.0) / 100.0
        }

        return ParsedReceiptData(
            totalCost = totalCost,
            gallons = gallons,
            pricePerGallon = pricePerGallon,
            stationName = stationName,
            defGallons = defGallons,
            defCost = defCost,
            odometer = odometer,
            state = state,
            dateText = dateText
        )
    }

    private val US_STATE_CODES = setOf(
        "AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "FL", "GA", "HI", "ID", "IL", "IN", "IA", "KS",
        "KY", "LA", "ME", "MD", "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ", "NM", "NY",
        "NC", "ND", "OH", "OK", "OR", "PA", "RI", "SC", "SD", "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY"
    )
}