package com.loadtracker.pro

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class DockTimeInfo(
    val arrivalTimeStr: String,
    val totalDockStr: String,
    val isBeforeWindow: Boolean,
    val clockMins: Long,
    val billableMins: Long,
    val apptText: String?
) {
    val isDetentionAccruing: Boolean get() = billableMins > 0L
    val isCallDispatchAlert: Boolean get() = clockMins >= CALL_DISPATCH_MINS && billableMins == 0L
    val freeMinsRemaining: Long get() = (FREE_MINS - clockMins).coerceAtLeast(0L)

    companion object {
        const val FREE_MINS = 120L
        const val CALL_DISPATCH_MINS = 90L
    }
}

object DockTimeCalculator {
    fun calculate(
        arrivalTs: Long,
        apptTs: Long?,
        apptType: String?,
        apptText: String?,
        nowMs: Long,
        zone: ZoneId = ZoneId.systemDefault()
    ): DockTimeInfo {
        val isBeforeWindow = apptType?.contains("before", ignoreCase = true) == true ||
            apptType?.contains("fcfs", ignoreCase = true) == true ||
            apptType.equals("B", ignoreCase = true)
        val clockStartTs = if (isBeforeWindow || apptTs == null) arrivalTs else maxOf(arrivalTs, apptTs)
        val totalDockMins = maxOf(0L, (nowMs - arrivalTs) / 60000L)
        val clockMins = maxOf(0L, (nowMs - clockStartTs) / 60000L)
        return DockTimeInfo(
            arrivalTimeStr = Instant.ofEpochMilli(arrivalTs).atZone(zone)
                .format(DateTimeFormatter.ofPattern("h:mm a", Locale.US)),
            totalDockStr = "${totalDockMins / 60}h ${totalDockMins % 60}m",
            isBeforeWindow = isBeforeWindow,
            clockMins = clockMins,
            billableMins = maxOf(0L, clockMins - DockTimeInfo.FREE_MINS),
            apptText = apptText
        )
    }

    fun detentionStatement(proNumber: String, facility: String, info: DockTimeInfo): String {
        val apptLabel = info.apptText
            ?: if (info.isBeforeWindow) "BEFORE / FCFS Window" else "Not Specified"
        return """
            🚨 DETENTION NOTICE - PRO #$proNumber
            Facility: $facility
            Arrival Time: ${info.arrivalTimeStr}
            Appt Schedule: $apptLabel
            Total Dock Time: ${info.totalDockStr}
            Billable Detention Accrued: ${info.billableMins / 60}h ${info.billableMins % 60}m
        """.trimIndent()
    }
}
