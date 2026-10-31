package com.loadtracker.pro

object OcrPayParser {
    // OCR can insert a space inside the number ("1 041.13"), so a single space counts as a thousands separator.
    private const val AMOUNT = """\$?\s*(\d{1,3}(?:[, ]\d{3})+|\d+)(?:\.(\d{1,2}))?"""

    private val grossPattern =
        Regex("""\bGross(?:\s*Pay)?\b\D*?$AMOUNT""", RegexOption.IGNORE_CASE)
    private val fallbackPattern =
        Regex("""\b(?:Pay|Rate|Linehaul|Total|Amount)\b\D*?$AMOUNT""", RegexOption.IGNORE_CASE)

    fun parse(text: String): String? {
        val match = grossPattern.find(text) ?: fallbackPattern.find(text) ?: return null
        val intPart = match.groupValues[1].replace(",", "").replace(" ", "")
        val decPart = match.groupValues[2].takeIf { it.isNotBlank() }?.let { ".$it" }.orEmpty()
        return intPart + decPart
    }
}
