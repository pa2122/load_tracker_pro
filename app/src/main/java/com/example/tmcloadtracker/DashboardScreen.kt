package com.example.tmcloadtracker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.jeziellago.compose.markdowntext.MarkdownText
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@Composable
fun DashboardScreen(
    summary: WeekSummary,
    pastLoads: List<CurrentLoad>,
    onAddNewLoadClick: () -> Unit,
    onUpdateTripClick: (CurrentLoad) -> Unit,
    onEditTripClick: (CurrentLoad) -> Unit,
    onDeleteTripClick: (CurrentLoad) -> Unit,
    liveBounceMiles: Double,
    liveLoadedMiles: Double,
    isTrainingActive: Boolean,
    flatTrainerPayRate: Double,
    showHelpOnLaunch: Boolean,
    onDismissHelpDialog: () -> Unit
) {
    val activeTrip = pastLoads.find { it.tripState != "COMPLETED" }
    val completedLoads = pastLoads.filter { it.tripState == "COMPLETED" }

    // Dialog control states
    var showCompletionDialog by remember { mutableStateOf(false) }
    var showHistoryDetailsDialog by remember { mutableStateOf(false) }
    var showWeeklyBreakdownDialog by remember { mutableStateOf(false) } 
    var selectedTripData by remember { mutableStateOf<CurrentLoad?>(null) }
    var showActiveOptionsDialog by remember { mutableStateOf(false) }
    var showStatementHistoryDialog by remember { mutableStateOf(false) }
    var selectedWeekFriday by remember { mutableStateOf<LocalDate?>(null) }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text("TMC Load Tracker", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Current Payroll Week Running Totals",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                // 1. RUNNING WEEKLY PERFORMANCE card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showWeeklyBreakdownDialog = true },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Current Week's Pay:")
                                Text(
                                    "$${String.format(Locale.US, "%.2f", summary.weeklyPay)}",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                    alpha = 0.2f
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Miles Driven This Week:")
                                Text("${summary.weeklyMilesDriven.toInt()} mi")
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Current Out-of-Route:")
                                Text(
                                    "${summary.weeklyOutOfRoute.toInt()} mi (${
                                        String.format(
                                            Locale.US,
                                            "%.1f",
                                            summary.weeklyOorPercentage
                                        )
                                    }%)"
                                )
                            }
                        }
                    }
                }

                // 2. ACTIVE RUN MILESTONE STEPPERS
                if (activeTrip != null) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showActiveOptionsDialog = true },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "⚠️ ACTIVE TRIP: PRO #${activeTrip.proNumber}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val label = when (activeTrip.tripState) {
                                    "ACTIVE_BOUNCE" -> "En Route to Shipper (Deadhead)"
                                    "ACTIVE_SHIPPER" -> "Loading / Tarping at Shipper Site"
                                    "ACTIVE_LOADED" -> "En Route to Consignee (Paid Transit)"
                                    else -> "Active Journey"
                                }
                                Text(label, style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Tracked Bounce")
                                        Text(
                                            "${String.format(Locale.US, "%.1f", liveBounceMiles)} mi",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Tracked Loaded")
                                        Text(
                                            "${String.format(Locale.US, "%.1f", liveLoadedMiles)} mi",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                when (activeTrip.tripState) {
                                    "ACTIVE_BOUNCE" -> {
                                        Button(
                                            onClick = {
                                                TrackingService.activeSegment = "Paused"
                                                onUpdateTripClick(
                                                    activeTrip.copy(
                                                        tripState = "ACTIVE_SHIPPER",
                                                        bounceMilesEnd = liveBounceMiles
                                                    )
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) { Text("Arrive at Shipper") }
                                    }

                                    "ACTIVE_SHIPPER" -> {
                                        Button(
                                            onClick = {
                                                TrackingService.activeSegment = "Loaded"
                                                onUpdateTripClick(activeTrip.copy(tripState = "ACTIVE_LOADED"))
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) { Text("Depart Shipper (Loaded)") }
                                    }

                                    "ACTIVE_LOADED" -> {
                                        Button(
                                            onClick = {
                                                selectedTripData = activeTrip.copy(
                                                    tripState = "COMPLETED",
                                                    loadedMilesEnd = liveLoadedMiles,
                                                    deliveryTimestamp = System.currentTimeMillis()
                                                )
                                                showCompletionDialog = true
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            modifier = Modifier.fillMaxWidth()
                                        ) { Text("Arrive at Consignee (Complete Load)") }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Button(
                            onClick = onAddNewLoadClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) { Text("Create New Load Entry") }
                    }
                }

                item {
                    Text(
                        "Past Load Logs (${completedLoads.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                item {
                    Button(
                        onClick = { showStatementHistoryDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("View Statements by Week")
                    }
                }

                if (completedLoads.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No completed logs recorded yet.",
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                } else {
                    items(items = completedLoads) { load ->
                        // 📍 THE UPGRADE: Formats the raw database timestamp into a crisp calendar date
                        val dateLabel = remember(load.pickupTimestamp) {
                            val instant = Instant.ofEpochMilli(load.pickupTimestamp)
                            val zone = ZoneId.systemDefault()
                            val formatter =
                                DateTimeFormatter.ofPattern("MM/dd/yyyy")
                            instant.atZone(zone).toLocalDate().format(formatter)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedTripData = load
                                    showCompletionDialog = false
                                    showWeeklyBreakdownDialog = false
                                    showHistoryDetailsDialog = true
                                }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "PRO #: ${load.proNumber}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    // 📍 VISUAL LAYOUT ROW INJECTION: Displays the structured pickup date
                                    Text(
                                        text = dateLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Gross Truck Pay: $${load.loadPay}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    val tarpLabel = when (load.tarpType) {
                                        "L" -> "Lumber"
                                        "S" -> "Steel"
                                        else -> "None"
                                    }
                                    Text(
                                        "Tarp: $tarpLabel",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // MODAL INTERFACE A: THE ORIGINAL ARRIVAL SETTLEMENT FORM BINDING
            if (showCompletionDialog && selectedTripData != null) {
                val trip = selectedTripData!!
                val baseGross = trip.loadPay
                val splitVal = trip.percentageRate
                val baseCut = baseGross * (splitVal / 100.0)
                var tarp = when (trip.tarpType) {
                    "L" -> 50.0
                    "S" -> 30.0
                    else -> 0.0
                }
                if (trip.isPreTarped) tarp /= 2.0
                val dhBonus =
                    if (trip.dispatchedBounceMiles >= 150.0) trip.dispatchedBounceMiles * 0.20 else 0.0
                val finalNet = baseCut + tarp + dhBonus

                AlertDialog(
                    onDismissRequest = { },
                    title = { Text("Load Settlement Summary - PRO #${trip.proNumber}") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Gross Truck Revenue:")
                                Text(
                                    "$${
                                        String.format(
                                            Locale.US,
                                            "%.2f",
                                            baseGross
                                        )
                                    }"
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Your Pay Split (${splitVal.toInt()}%):")
                                Text(
                                    "$${
                                        String.format(
                                            Locale.US,
                                            "%.2f",
                                            baseCut
                                        )
                                    }"
                                )
                            }
                            if (tarp > 0.0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Tarp Pay Addon:")
                                    Text(
                                        "+$${
                                            String.format(
                                                Locale.US,
                                                "%.2f",
                                                tarp
                                            )
                                        }"
                                    )
                                }
                            }
                            if (dhBonus > 0.0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Long Deadhead Bonus:")
                                    Text(
                                        "+$${
                                            String.format(
                                                Locale.US,
                                                "%.2f",
                                                dhBonus
                                            )
                                        }"
                                    )
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Total Net Take-Home Pay:",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    "$${
                                        String.format(
                                            Locale.US,
                                            "%.2f",
                                            finalNet
                                        )
                                    }",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            onUpdateTripClick(trip); showCompletionDialog =
                            false; selectedTripData = null
                        }) { Text("Confirm & File Log") }
                    }
                )
            }

            // 📍 MODAL INTERFACE B: THE BRAND NEW PAST LOAD LOG DETAILED BREAKDOWN ACCOUNTING STATEMENT
            if (showHistoryDetailsDialog && selectedTripData != null) {
                val trip = selectedTripData!!
                val baseGross = trip.loadPay
                val splitVal = trip.percentageRate
                val baseCut = baseGross * (splitVal / 100.0)
                var tarp = when (trip.tarpType) {
                    "L" -> 50.0
                    "S" -> 30.0
                    else -> 0.0
                }
                if (trip.isPreTarped) tarp /= 2.0
                val dhBonus =
                    if (trip.dispatchedBounceMiles >= 150.0) trip.dispatchedBounceMiles * 0.20 else 0.0
                val finalNet = baseCut + tarp + dhBonus

                // Mileage performance variance math
                val actualBounceRun = trip.bounceMilesEnd - trip.bounceMilesStart
                val actualLoadedRun = trip.loadedMilesEnd - trip.loadedMilesStart
                val totalActualTripDriven = actualBounceRun + actualLoadedRun
                val totalDispatchedTripExpected =
                    trip.dispatchedBounceMiles + trip.dispatchedLoadedMiles
                val tripOorVariance = totalActualTripDriven - totalDispatchedTripExpected

                AlertDialog(
                    onDismissRequest = {
                        showHistoryDetailsDialog = false; selectedTripData = null
                    },
                    title = { Text("Historical Record: PRO #${trip.proNumber}") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "Financial Payroll Statement",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Gross Truck Revenue:")
                                Text(
                                    "$${
                                        String.format(
                                            Locale.US,
                                            "%.2f",
                                            baseGross
                                        )
                                    }"
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Your Percentage Share (${splitVal.toInt()}%):")
                                Text(
                                    "$${
                                        String.format(
                                            Locale.US,
                                            "%.2f",
                                            baseCut
                                        )
                                    }"
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tarp Allowance:")
                                Text("$${String.format(Locale.US, "%.2f", tarp)}")
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Deadhead Bonus Pay:")
                                Text(
                                    "$${
                                        String.format(
                                            Locale.US,
                                            "%.2f",
                                            dhBonus
                                        )
                                    }"
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Net Statement Earnings:",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    "$${
                                        String.format(
                                            Locale.US,
                                            "%.2f",
                                            finalNet
                                        )
                                    }",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Text(
                                "Mileage Performance Audit",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Dispatched Target Route:")
                                Text("${totalDispatchedTripExpected.toInt()} mi")
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Actual GPS Driven Route:")
                                Text("${totalActualTripDriven.toInt()} mi")
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Out-of-Route Variance:")
                                val oorDisplay =
                                    if (trip.isGoingHome) "0 mi (Home Run)" else "${if (tripOorVariance > 0) tripOorVariance.toInt() else 0} mi"
                                Text(oorDisplay)
                            }

                            if (!trip.tripNotes.isNullOrBlank()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Text(
                                    "Trip Notes",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    trip.tripNotes,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showHistoryDetailsDialog = false; selectedTripData = null
                        }) { Text("Close Statement") }
                    }
                )
            }
        }
    }
    // 📍 NEW MODAL INTERFACE C: DETAILED WEEKLY PAYROLL STATEMENT BREAKDOWN POPUP
    if (showWeeklyBreakdownDialog) {
        AlertDialog(
            onDismissRequest = { showWeeklyBreakdownDialog = false },
            title = { Text("Weekly Earnings Breakdown") },
            text = {
                val currentTargetFriday = Instant.ofEpochMilli(System.currentTimeMillis())
                    .atZone(ZoneId.systemDefault()).toLocalDate()
                    .let { d ->
                        when (d.dayOfWeek) {
                            DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> d.with(
                                TemporalAdjusters.next(DayOfWeek.FRIDAY)
                            )

                            else -> d.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY))
                        }
                    }

                val weeklyTrips = pastLoads.filter { load ->
                    val loadFriday = Instant.ofEpochMilli(load.pickupTimestamp)
                        .atZone(ZoneId.systemDefault()).toLocalDate()
                        .let { d ->
                            when (d.dayOfWeek) {
                                DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> d.with(
                                    TemporalAdjusters.next(DayOfWeek.FRIDAY)
                                )

                                else -> d.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY))
                            }
                        }
                    loadFriday == currentTargetFriday && load.tripState == "COMPLETED"
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    item {
                        Text(
                            "Individual Load Logs",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    if (weeklyTrips.isEmpty()) {
                        item {
                            Text(
                                "No completed loads filed for this pay week.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    } else {
                        items(items = weeklyTrips) { trip ->
                            val baseDriverCut = trip.loadPay * (trip.percentageRate / 100.0)
                            var tarpAddon = when (trip.tarpType) {
                                "L" -> 50.0
                                "S" -> 30.0
                                else -> 0.0
                            }
                            if (trip.isPreTarped) tarpAddon /= 2.0
                            val bounceBonus =
                                if (trip.dispatchedBounceMiles >= 150.0) trip.dispatchedBounceMiles * 0.20 else 0.0
                            val totalTripPay = baseDriverCut + tarpAddon + bounceBonus

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        "PRO #: ${trip.proNumber}",
                                        style = MaterialTheme.typography.titleSmall
                                    )

                                    // 📍 FIX A: Separated into clean standalone stacked lines
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "Truck Gross Pay:",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            "$${String.format(Locale.US, "%.2f", trip.loadPay)}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "Your Split (${trip.percentageRate.toInt()}%):",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            "$${String.format(Locale.US, "%.2f", baseDriverCut)}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    if (tarpAddon > 0.0 || bounceBonus > 0.0) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "Extras (Tarp/Bounce):",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Text(
                                                "+$${
                                                    String.format(
                                                        Locale.US,
                                                        "%.2f",
                                                        tarpAddon + bounceBonus
                                                    )
                                                }",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.1f
                                        )
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "Total Load Net:",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            "$${String.format(Locale.US, "%.2f", totalTripPay)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            "Weekly Configuration Status",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        var calculatedLoadAccumulations = 0.0
                        weeklyTrips.forEach { t ->
                            val cut = t.loadPay * (t.percentageRate / 100.0)
                            var tarp = when (t.tarpType) {
                                "L" -> 50.0
                                "S" -> 30.0
                                else -> 0.0
                            }
                            if (t.isPreTarped) tarp /= 2.0
                            val dh =
                                if (t.dispatchedBounceMiles >= 150.0) t.dispatchedBounceMiles * 0.20 else 0.0
                            calculatedLoadAccumulations += (cut + tarp + dh)
                        }

                        // 📍 FIX B: Clean line displaying your Training status explicitly
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Training:")
                            Text(
                                if (isTrainingActive) "Yes" else "No",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (isTrainingActive) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Flat Trainer Premium Addon:")
                                Text(
                                    "+$${String.format(Locale.US, "%.2f", flatTrainerPayRate)}",
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Combined Statement Total:",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                "$${String.format(Locale.US, "%.2f", summary.weeklyPay)}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showWeeklyBreakdownDialog = false }) {
                    Text("Dismiss Statement")
                }
            }
        )
    }

    if (showActiveOptionsDialog && activeTrip != null) {
        AlertDialog(
            onDismissRequest = { showActiveOptionsDialog = false },
            title = { Text("Trip Management - PRO #${activeTrip.proNumber}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select an action for your currently active load.")
                    
                    Button(
                        onClick = {
                            showActiveOptionsDialog = false
                            onEditTripClick(activeTrip)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) { Text("Edit Trip Details") }

                    Button(
                        onClick = {
                            showActiveOptionsDialog = false
                            onDeleteTripClick(activeTrip)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Cancel / Delete Trip") }
                }
            },
            confirmButton = {
                TextButton(onClick = { showActiveOptionsDialog = false }) { Text("Close") }
            }
        )
    }

    if (showStatementHistoryDialog) {
        val grouped = remember(pastLoads) {
            pastLoads.filter { it.tripState == "COMPLETED" }
                .groupBy { load ->
                    Instant.ofEpochMilli(load.pickupTimestamp)
                        .atZone(ZoneId.systemDefault()).toLocalDate()
                        .let { d ->
                            when (d.dayOfWeek) {
                                DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> d.with(TemporalAdjusters.next(DayOfWeek.FRIDAY))
                                else -> d.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY))
                            }
                        }
                }.toSortedMap(reverseOrder())
        }

        AlertDialog(
            onDismissRequest = { showStatementHistoryDialog = false },
            title = { Text("Historical Payroll Statements") },
            text = {
                Box(modifier = Modifier.heightIn(max = 500.dp)) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (grouped.isEmpty()) {
                            item { Text("No historical records found.") }
                        } else {
                            grouped.forEach { (friday, loads) ->
                                item {
                                    val totalWeekPay = loads.sumOf { load ->
                                        val base = load.loadPay * (load.percentageRate / 100.0)
                                        val bonus = if (load.dispatchedBounceMiles >= 150.0) load.dispatchedBounceMiles * 0.20 else 0.0
                                        // Simplified pay (doesn't account for trainer/manual rates here, just a summary)
                                        base + bonus
                                    }
                                    Card(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            selectedWeekFriday = friday
                                        },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("Week Ending:", style = MaterialTheme.typography.labelSmall)
                                                Text(friday.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")), style = MaterialTheme.typography.titleMedium)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("Total Pay (Est):", style = MaterialTheme.typography.labelSmall)
                                                Text("$${String.format(Locale.US, "%.2f", totalWeekPay)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatementHistoryDialog = false }) { Text("Close") }
            }
        )
    }

    if (selectedWeekFriday != null) {
        val weekFriday = selectedWeekFriday!!
        val weeklyTrips = pastLoads.filter { load ->
            val loadFriday = Instant.ofEpochMilli(load.pickupTimestamp)
                .atZone(ZoneId.systemDefault()).toLocalDate()
                .let { d ->
                    when (d.dayOfWeek) {
                        DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> d.with(TemporalAdjusters.next(DayOfWeek.FRIDAY))
                        else -> d.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY))
                    }
                }
            loadFriday == weekFriday && load.tripState == "COMPLETED"
        }

        AlertDialog(
            onDismissRequest = { selectedWeekFriday = null },
            title = { Text("Statement: ${weekFriday.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))}") },
            text = {
                Box(modifier = Modifier.heightIn(max = 500.dp)) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(weeklyTrips) { trip ->
                            val net = (trip.loadPay * (trip.percentageRate / 100.0)) + (if (trip.dispatchedBounceMiles >= 150.0) trip.dispatchedBounceMiles * 0.20 else 0.0)
                            Card(modifier = Modifier.fillMaxWidth().clickable { 
                                selectedTripData = trip
                                showHistoryDetailsDialog = true
                            }) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("PRO #${trip.proNumber}", style = MaterialTheme.typography.titleSmall)
                                        Text("$${String.format(Locale.US, "%.2f", net)}", color = MaterialTheme.colorScheme.primary)
                                    }
                                    Text("${trip.dispatchedLoadedMiles.toInt()} mi Loaded", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedWeekFriday = null }) { Text("Back to List") }
            }
        )
    }

// 📍 POPUP DIALOG THAT READS YOUR RAW ASSETS FILE AT RUNTIME
    if (showHelpOnLaunch) {
        val context = LocalContext.current

        // Safely reads the text lines from your assets directory folder
        val helpTextString = remember {
            try {
                // Updated file path target to look for your new .md package file
                context.assets.open("help_guide.md").bufferedReader().use { it.readText() }
            } catch (_: Exception) {
                "# Error\nCould not locate your `help_guide.md` asset file. Please check folder placement."
            }
        }

        AlertDialog(
            onDismissRequest = onDismissHelpDialog,
            title = { Text("App Reference Manual & Todo") },
            text = {
                Box(modifier = Modifier.heightIn(max = 400.dp)) {
                    LazyColumn {
                        item {
                            // 📍 THE UPGRADE: Swaps plain Text for dynamic stylized Markdown Rendering
                            MarkdownText(
                                markdown = helpTextString,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = onDismissHelpDialog) {
                    Text("Close Manual")
                }
            }
        )
    }

} 
