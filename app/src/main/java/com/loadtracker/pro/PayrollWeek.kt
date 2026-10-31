package com.loadtracker.pro

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

// Pay weeks run Monday to Sunday and follow the delivery date.
fun getPayrollWeekStartDate(
    timestamp: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): LocalDate = Instant.ofEpochMilli(timestamp)
    .atZone(zoneId)
    .toLocalDate()
    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

fun CurrentLoad.payrollWeekStartDate(
    zoneId: ZoneId = ZoneId.systemDefault()
): LocalDate? {
    if (tripState != "COMPLETED") return null
    // Older completed loads predate deliveryTimestamp; retain their totals using pickup date.
    return getPayrollWeekStartDate(deliveryTimestamp ?: pickupTimestamp, zoneId)
}

// Pay follows delivery, so every load list shows the most recently delivered load first.
fun List<CurrentLoad>.sortedByNewestDelivery(): List<CurrentLoad> =
    sortedWith(
        compareByDescending<CurrentLoad> { it.deliveryTimestamp ?: it.pickupTimestamp }
            .thenByDescending { it.pickupTimestamp }
    )

fun formatLoadDates(
    load: CurrentLoad,
    zoneId: ZoneId = ZoneId.systemDefault()
): String {
    val formatter = java.time.format.DateTimeFormatter.ofPattern("EEE MM/dd", java.util.Locale.US)
    fun Long.fmt() = Instant.ofEpochMilli(this).atZone(zoneId).format(formatter)
    val picked = load.pickupTimestamp.fmt()
    val delivered = load.deliveryTimestamp?.fmt() ?: return "Picked up $picked"
    return "Picked up $picked → Delivered $delivered"
}
