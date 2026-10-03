package com.loadtracker.pro

import java.time.Instant
import java.time.ZoneId
import java.util.Locale

object IftaTaxReportExporter {

    data class StateIftaSummary(
        val stateCode: String,
        val milesDriven: Double,
        val gallonsPumped: Double,
        val totalCost: Double
    )

    /**
     * Generates CSV content for quarterly IFTA tax audit report.
     */
    fun generateIftaCsv(
        quarterLabel: String,
        fuelEntries: List<FuelEntry>,
        breadcrumbs: List<TripBreadcrumb>
    ): String {
        val stateMilesMap = IftaStateDetector.calculateStateMileage(breadcrumbs)
        val stateFuelMap = fuelEntries.groupBy { it.state.uppercase(Locale.US) }

        val allStates = (stateMilesMap.keys + stateFuelMap.keys).toSet().sorted()

        val sb = StringBuilder()
        sb.append("Quarter,State,Miles Driven,Gallons Pumped,Total Fuel Cost ($),Net Taxable Balance\n")

        allStates.forEach { state ->
            val miles = stateMilesMap[state] ?: 0.0
            val entries = stateFuelMap[state] ?: emptyList()
            val gallons = entries.sumOf { it.gallons }
            val cost = entries.sumOf { it.totalCost }

            val netBalance = miles - (gallons * 6.5) // Standard 6.5 MPG baseline

            sb.append("\"$quarterLabel\",")
            sb.append("\"$state\",")
            sb.append("${String.format(Locale.US, "%.1f", miles)},")
            sb.append("${String.format(Locale.US, "%.1f", gallons)},")
            sb.append("${String.format(Locale.US, "%.2f", cost)},")
            sb.append("${String.format(Locale.US, "%.1f", netBalance)}\n")
        }

        return sb.toString()
    }
}
