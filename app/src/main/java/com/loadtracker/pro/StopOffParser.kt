package com.loadtracker.pro

internal object StopOffParser {
    private val stopOffCountPattern = Regex(
        """\bstop[\s-]*offs?\b\s*(?:[:=#-]\s*)?(\d{1,9})\b""",
        RegexOption.IGNORE_CASE
    )
    private val stopOffLabelPattern = Regex(
        """^\s*stop[\s-]*offs?\b""",
        RegexOption.IGNORE_CASE
    )
    private val routeStopNamePattern = Regex(
        """^\s*stop[\s-]*off\s*#?(\d+)\s*(?:name)?\s*:\s*(.+?)\s*$""",
        RegexOption.IGNORE_CASE
    )
    private val routeStopAddressPattern = Regex(
        """^\s*stop[\s-]*off\s*#?(\d+)\s+address\s*:\s*(.+?)\s*$""",
        RegexOption.IGNORE_CASE
    )
    private val routeStopMilesPattern = Regex(
        """^\s*stop[\s-]*off\s*#?(\d+)\s+(?:dispatched\s+)?(?:leg\s+)?miles?\s*:\s*([\d,.]+)\s*$""",
        RegexOption.IGNORE_CASE
    )

    fun parseCount(text: String): Int? =
        stopOffCountPattern.find(text)?.groupValues?.get(1)?.toIntOrNull()

    fun isLabelLine(line: String): Boolean = stopOffLabelPattern.containsMatchIn(line)

    fun parseRouteStops(text: String): List<LoadStopDraft> {
        data class StopFields(
            var name: String = "",
            var address: String = "",
            var miles: String = ""
        )

        val fields = sortedMapOf<Int, StopFields>()
        text.lineSequence().forEach { line ->
            routeStopNamePattern.find(line)?.let { match ->
                match.groupValues[1].toIntOrNull()?.takeIf { it in 1..20 }?.let { index ->
                    fields.getOrPut(index) { StopFields() }.name = match.groupValues[2].trim()
                }
            }
            routeStopAddressPattern.find(line)?.let { match ->
                match.groupValues[1].toIntOrNull()?.takeIf { it in 1..20 }?.let { index ->
                    fields.getOrPut(index) { StopFields() }.address = match.groupValues[2].trim()
                }
            }
            routeStopMilesPattern.find(line)?.let { match ->
                match.groupValues[1].toIntOrNull()?.takeIf { it in 1..20 }?.let { index ->
                    fields.getOrPut(index) { StopFields() }.miles =
                        match.groupValues[2].replace(",", "")
                }
            }
        }
        return fields.values.map { stop ->
            LoadStopDraft(
                name = stop.name,
                address = stop.address,
                latitude = null,
                longitude = null,
                dispatchedLegMiles = stop.miles
            )
        }
    }
}
