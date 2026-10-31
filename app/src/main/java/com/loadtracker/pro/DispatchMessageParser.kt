package com.loadtracker.pro

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal data class ParsedAppointment(
    val text: String,
    val type: String,
    val timestamp: Long?,
    val inFirstHalf: Boolean
)

internal enum class DispatchField(@androidx.annotation.StringRes val labelRes: Int) {
    PRO_NUMBER(R.string.dispatch_field_pro),
    PICKUP_FACILITY(R.string.dispatch_field_pickup_facility),
    DELIVERY_FACILITY(R.string.dispatch_field_delivery_facility),
    PICKUP_DATE(R.string.dispatch_field_pickup_date),
    PAY(R.string.dispatch_field_pay),
    LOADED_MILES(R.string.dispatch_field_loaded_miles),
    BOUNCE_MILES(R.string.dispatch_field_bounce_miles)
}

internal data class ParsedDispatch(
    val proNumber: String?,
    val loadPay: String?,
    val dispatchedLoadedMiles: String?,
    val dispatchedBounceMiles: String?,
    val extraStopCount: Int?,
    val routeStops: List<LoadStopDraft>,
    val deliveryDateMillis: Long?,
    val shipperText: String,
    val consigneeText: String,
    val shipperNotes: FacilityNoteFields,
    val consigneeNotes: FacilityNoteFields,
    val appointments: List<ParsedAppointment>,
    val legacyPickupDateMillis: Long?,
    val isGoingHome: Boolean?,
    val tarp: OcrTarpInfo?
) {
    val pickupAppointment: ParsedAppointment? = appointments.firstOrNull { it.inFirstHalf }
    val deliveryAppointment: ParsedAppointment? = appointments.firstOrNull { it !== pickupAppointment }
    val pickupDateMillis: Long? = pickupAppointment?.timestamp ?: legacyPickupDateMillis

    // A screenshot that was cut off will be missing some of these.
    val missingFields: List<DispatchField> = listOfNotNull(
        DispatchField.PRO_NUMBER.takeIf { proNumber.isNullOrBlank() },
        DispatchField.PICKUP_FACILITY.takeIf { shipperText.isBlank() },
        DispatchField.DELIVERY_FACILITY.takeIf { consigneeText.isBlank() },
        DispatchField.PICKUP_DATE.takeIf { pickupDateMillis == null },
        DispatchField.PAY.takeIf { loadPay.isNullOrBlank() },
        DispatchField.LOADED_MILES.takeIf { dispatchedLoadedMiles.isNullOrBlank() },
        DispatchField.BOUNCE_MILES.takeIf { dispatchedBounceMiles.isNullOrBlank() }
    )
}

// The single parser behind both the Start Load screenshot scan and the bulk screenshot import.
internal object DispatchMessageParser {
    private val proPattern =
        Regex("""\b(?:PRO|Order|Load|Trip)\s*(?:Number|No|Num)?\s*#?\s*:?\s*(\d{4,12})""", RegexOption.IGNORE_CASE)
    private val loadedPattern = Regex(
        """(?:loaded\s*miles|loaded\s*mi|load\s*miles|trip\s*miles|distance)\s*[:=\-\s]*(\d{1,4}(?:,\d{3})*(?:\.\d+)?)""",
        RegexOption.IGNORE_CASE
    )
    private val bouncePattern = Regex(
        """(?:bounce\s*miles|bounce\s*mi|deadhead\s*miles|deadhead\s*mi|dh\s*miles)\s*[:=\-\s]*(\d{1,4}(?:,\d{3})*(?:\.\d+)?)""",
        RegexOption.IGNORE_CASE
    )
    private val numberPattern = Regex("""\b\d{1,4}(?:,\d{3})*(?:\.\d+)?\b""")
    private val goingHomePattern =
        Regex("""(?:Going\s*Home|Home\s*Run|Home)\s*[:=\-\s]*([YN])""", RegexOption.IGNORE_CASE)
    private val apptPattern = Regex(
        """(?:[@B]\s*)?(\d{1,2}/\d{1,2}/\d{2,4}\s+\d{1,2}:\d{2}(?::\d{2})?\s*(?:AM|PM)?)""",
        RegexOption.IGNORE_CASE
    )
    private val stopTypePattern = Regex("""^\s*stop\s*type\s*:\s*(.*)$""", RegexOption.IGNORE_CASE)
    private val anyLabelPattern = Regex("""^[A-Za-z][A-Za-z /#]{1,30}:""")
    private val simpleDatePattern =
        Regex("""\b(?:\d{1,2}[/-]\d{1,2}[/-]\d{2,4}|\d{4}-\d{1,2}-\d{1,2})\b""")
    private val shipperHeader = Regex(
        """^(shipper|pickup|origin)(?:\s+(?:name|location|address))?\s*:?\s*(.*)$""",
        RegexOption.IGNORE_CASE
    )
    private val consigneeHeader = Regex(
        """^(consignee|delivery|destination|dropoff)(?:\s+(?:name|location|address))?\s*:?\s*(.*)$""",
        RegexOption.IGNORE_CASE
    )
    private const val MAX_FACILITY_LINES = 3

    fun parse(rawText: String, zoneId: ZoneId = ZoneId.systemDefault()): ParsedDispatch {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val structured = lines.any { stopTypePattern.containsMatchIn(it) }
        val (shipperText, consigneeText) =
            if (structured) structuredFacilities(lines) else legacyFacilities(lines)
        val (shipperNotes, consigneeNotes) = facilityNotes(lines, structured)

        val (loaded, bounce) = parseMiles(rawText, lines)
        return ParsedDispatch(
            proNumber = proPattern.find(rawText)?.groupValues?.get(1)
                ?: Regex("""\b\d{6,10}\b""").find(rawText)?.value,
            loadPay = OcrPayParser.parse(rawText),
            dispatchedLoadedMiles = loaded,
            dispatchedBounceMiles = bounce,
            extraStopCount = StopOffParser.parseCount(rawText),
            routeStops = StopOffParser.parseRouteStops(rawText),
            deliveryDateMillis = OcrDateParser.extractDeliveryDateMillis(rawText, zoneId),
            shipperText = shipperText,
            consigneeText = consigneeText,
            shipperNotes = shipperNotes,
            consigneeNotes = consigneeNotes,
            appointments = parseAppointments(lines, zoneId, structured),
            legacyPickupDateMillis = legacyPickupDate(lines, zoneId),
            isGoingHome = goingHomePattern.find(rawText)?.groupValues?.get(1)?.equals("Y", ignoreCase = true),
            tarp = OcrTarpParser.parse(rawText)
        )
    }

    private fun parseMiles(rawText: String, lines: List<String>): Pair<String?, String?> {
        var loaded = loadedPattern.find(rawText)?.groupValues?.get(1)?.replace(",", "")
        var bounce = bouncePattern.find(rawText)?.groupValues?.get(1)?.replace(",", "")
        if (loaded == null || bounce == null) {
            for (line in lines) {
                val lower = line.lowercase(Locale.US)
                if (lower.contains("out of route") || lower.contains("po number") ||
                    lower.contains("order") || lower.contains("status")
                ) continue
                if (loaded == null && (lower.contains("loaded miles") || lower.contains("loaded mi"))) {
                    loaded = numberPattern.find(line)?.value?.replace(",", "")
                }
                if (bounce == null && (lower.contains("bounce miles") || lower.contains("bounce mi") ||
                            lower.contains("deadhead miles") || lower.contains("dh miles"))
                ) {
                    bounce = numberPattern.find(line)?.value?.replace(",", "")
                }
            }
        }
        return loaded to bounce
    }

    private fun stopRole(type: String): String? {
        val lower = type.lowercase(Locale.US)
        return when {
            lower.contains("origin") || lower.contains("pickup") -> "shipper"
            lower.contains("final") || lower.contains("dropoff") || lower.contains("drop off") ||
                    lower.contains("delivery") || lower.contains("consignee") -> "consignee"
            else -> null
        }
    }

    // Dispatch messages list each stop as "Stop Type: ..." followed by the name and address lines.
    private fun structuredFacilities(lines: List<String>): Pair<String, String> {
        var shipper = ""
        var consignee = ""
        lines.forEachIndexed { index, line ->
            val type = stopTypePattern.find(line)?.groupValues?.get(1) ?: return@forEachIndexed
            val role = stopRole(type) ?: return@forEachIndexed
            val block = lines.drop(index + 1)
                .takeWhile { !stopTypePattern.containsMatchIn(it) && !apptPattern.containsMatchIn(it) && !anyLabelPattern.containsMatchIn(it) }
                .take(MAX_FACILITY_LINES)
                .joinToString("\n")
            if (role == "shipper" && shipper.isBlank()) shipper = block
            if (role == "consignee" && consignee.isBlank()) consignee = block
        }
        return shipper to consignee
    }

    private fun legacyFacilities(lines: List<String>): Pair<String, String> {
        var current: String? = null
        val shipper = StringBuilder()
        val consignee = StringBuilder()
        for (trimmed in lines) {
            val lower = trimmed.lowercase(Locale.US)
            val shipperMatch = shipperHeader.find(trimmed)
            val consigneeMatch = consigneeHeader.find(trimmed)
            when {
                lower.startsWith("pickup date") || lower.startsWith("pickup appt") ||
                        lower.startsWith("pickup appointment") || lower.startsWith("delivery date") ||
                        lower.startsWith("delivery appt") || lower.startsWith("delivery appointment") ||
                        lower.startsWith("going home") || lower.startsWith("home run") ||
                        StopOffParser.isLabelLine(trimmed) || lower.startsWith("tarp") -> current = null
                shipperMatch != null -> {
                    current = "shipper"
                    shipperMatch.groupValues[2].takeIf { it.isNotBlank() }?.let { shipper.append(it.trim()).append('\n') }
                }
                consigneeMatch != null -> {
                    current = "consignee"
                    consigneeMatch.groupValues[2].takeIf { it.isNotBlank() }?.let { consignee.append(it.trim()).append('\n') }
                }
                lower.startsWith("pro ") || lower.startsWith("gross ") ||
                        lower.startsWith("bounce ") || lower.startsWith("loaded ") ||
                        lower.startsWith("stop type") -> current = null
                current == "shipper" && shipper.lines().size <= MAX_FACILITY_LINES ->
                    shipper.append(trimmed).append('\n')
                current == "consignee" && consignee.lines().size <= MAX_FACILITY_LINES ->
                    consignee.append(trimmed).append('\n')
            }
        }
        return shipper.toString().trim() to consignee.toString().trim()
    }

    private fun facilityNotes(lines: List<String>, structured: Boolean): Pair<FacilityNoteFields, FacilityNoteFields> {
        var facility = ""
        var field = ""
        val instructions = mutableMapOf("shipper" to StringBuilder(), "consignee" to StringBuilder())
        val directions = mutableMapOf("shipper" to StringBuilder(), "consignee" to StringBuilder())
        val parking = mutableMapOf("shipper" to "", "consignee" to "")

        for (line in lines) {
            val lower = line.lowercase(Locale.US)
            val stopType = stopTypePattern.find(line)?.groupValues?.get(1)
            when {
                structured && stopType != null -> { facility = stopRole(stopType).orEmpty(); field = "" }
                !structured && (lower.contains("shipper") || lower.contains("pickup") || lower.contains("origin")) -> {
                    facility = "shipper"; field = ""
                }
                !structured && (lower.contains("consignee") || lower.contains("delivery") ||
                        lower.contains("destination") || lower.contains("drop")) -> {
                    facility = "consignee"; field = ""
                }
                StopOffParser.isLabelLine(line) || lower.contains("pro number") || lower.contains("load number") ||
                        lower.contains("gross:") || lower.contains("additional load notes") -> {
                    facility = ""; field = ""
                }
            }
            val content = line.substringAfter(":").trim()
            when {
                lower.startsWith("instructions:") || lower.startsWith("instruction:") -> {
                    field = "instructions"
                    if (content.isNotBlank()) instructions[facility]?.append(content)?.append(" ")
                }
                lower.startsWith("directions:") || lower.startsWith("direction:") -> {
                    field = "directions"
                    if (content.isNotBlank()) directions[facility]?.append(content)?.append(" ")
                }
                lower.startsWith("parking:") -> {
                    field = "parking"
                    if (facility in parking) parking[facility] = content
                }
                field == "instructions" && !line.contains(":") -> instructions[facility]?.append(line)?.append(" ")
                field == "directions" && !line.contains(":") -> directions[facility]?.append(line)?.append(" ")
                field == "parking" && !line.contains(":") && facility in parking && parking.getValue(facility).isBlank() ->
                    parking[facility] = line
            }
        }
        fun fieldsFor(role: String) = FacilityNoteFields(
            instructions.getValue(role).toString().trim(),
            directions.getValue(role).toString().trim(),
            parking.getValue(role).trim()
        )
        return fieldsFor("shipper") to fieldsFor("consignee")
    }

    // In structured messages an appointment belongs to the stop block above it, so a screenshot
    // that only shows the drop-off never reports its date as the pickup.
    private fun parseAppointments(lines: List<String>, zoneId: ZoneId, structured: Boolean): List<ParsedAppointment> {
        var role: String? = null
        return lines.mapIndexedNotNull { index, line ->
            stopTypePattern.find(line)?.let { role = stopRole(it.groupValues[1]) }
            val match = apptPattern.find(line) ?: return@mapIndexedNotNull null
            ParsedAppointment(
                text = line,
                type = if (line.startsWith("B", ignoreCase = true)) "BEFORE" else "EXACT",
                timestamp = parseApptDateToMillis(match.groupValues[1].trim(), zoneId),
                inFirstHalf = if (structured) role == "shipper" else index < lines.size / 2
            )
        }
    }

    fun parseApptDateToMillis(dateStr: String, zoneId: ZoneId = ZoneId.systemDefault()): Long? {
        val patterns = listOf("M/d/yyyy h:mm:ss a", "M/d/yyyy h:mm a", "MM/dd/yyyy HH:mm:ss", "MM/dd/yyyy HH:mm")
        for (pattern in patterns) {
            try {
                val ldt = LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern(pattern, Locale.US))
                return ldt.atZone(zoneId).toInstant().toEpochMilli()
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun legacyPickupDate(lines: List<String>, zoneId: ZoneId): Long? {
        val dateText = lines.firstNotNullOfOrNull { line ->
            if (Regex("""\b(pick[\s-]?up|origin|shipper)\b""", RegexOption.IGNORE_CASE).containsMatchIn(line)) {
                simpleDatePattern.find(line)?.value
            } else null
        } ?: return null
        val patterns = listOf("M/d/yyyy", "M-d-yyyy", "M/d/yy", "M-d-yy", "yyyy-M-d")
        val date = patterns.firstNotNullOfOrNull { pattern ->
            try {
                LocalDate.parse(dateText, DateTimeFormatter.ofPattern(pattern, Locale.US))
            } catch (_: Exception) {
                null
            }
        } ?: return null
        return date.atTime(12, 0).atZone(zoneId).toInstant().toEpochMilli()
    }
}
