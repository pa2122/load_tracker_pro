package com.loadtracker.pro

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

object OcrDateParser {
    private val datePattern =
        Regex("""\b(\d{4}-\d{1,2}-\d{1,2}|\d{1,2}[/-]\d{1,2}[/-]\d{2,4})\b""")
    private val dateFormatters = listOf(
        DateTimeFormatter.ofPattern("M/d/uuuu", Locale.US),
        DateTimeFormatter.ofPattern("M-d-uuuu", Locale.US),
        DateTimeFormatter.ofPattern("M/d/yy", Locale.US),
        DateTimeFormatter.ofPattern("M-d-yy", Locale.US),
        DateTimeFormatter.ofPattern("uuuu-M-d", Locale.US)
    )

    fun extractDeliveryDateMillis(
        text: String,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long? {
        val lines = text.lines()
        for (line in lines) {
            val normalized = line.lowercase(Locale.US)
            if (!Regex("""\b(delivery|delivered|drop(?:off)?)\b""").containsMatchIn(normalized)) continue
            if (Regex("""\b(appointment|appt|scheduled|window)\b""").containsMatchIn(normalized)) continue

            parseDate(line)?.let { return it.atTime(12, 0).atZone(zoneId).toInstant().toEpochMilli() }
        }

        // Dispatch screenshots place the final-drop appointment on a separate "B" line.
        val finalDropoffIndex = lines.indexOfFirst { line ->
            Regex("""\bstop\s*type\s*:\s*final\s*drop\s*off\b""", RegexOption.IGNORE_CASE)
                .containsMatchIn(line)
        }
        if (finalDropoffIndex >= 0) {
            val finalDropoffLines = lines.drop(finalDropoffIndex + 1)
                .takeWhile { !it.contains("Stop Type", ignoreCase = true) }
            for (line in finalDropoffLines) {
                if (!Regex("""^\s*B\b""", RegexOption.IGNORE_CASE).containsMatchIn(line)) continue
                parseDate(line)?.let { return it.atTime(12, 0).atZone(zoneId).toInstant().toEpochMilli() }
            }
        }
        return null
    }

    private fun parseDate(text: String): LocalDate? {
        val dateText = datePattern.find(text)?.groupValues?.get(1) ?: return null
        return dateFormatters.firstNotNullOfOrNull { formatter ->
            try {
                LocalDate.parse(dateText, formatter)
            } catch (_: Exception) {
                null
            }
        }
    }

    fun datePickerMillisForTimestamp(timestamp: Long, zoneId: ZoneId): Long =
        Instant.ofEpochMilli(timestamp)
            .atZone(zoneId)
            .toLocalDate()
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

    fun timestampForDatePickerMillis(datePickerMillis: Long, zoneId: ZoneId): Long =
        Instant.ofEpochMilli(datePickerMillis)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
            .atTime(12, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
}
