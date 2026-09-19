package com.example.tmcloadtracker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.jeziellago.compose.markdowntext.MarkdownText
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    summary: WeekSummary,
    pastLoads: List<CurrentLoad>,
    isProUser: Boolean,
    onAddNewLoadClick: () -> Unit,
    onUpdateTripClick: (CurrentLoad) -> Unit,
    onEditTripClick: (CurrentLoad) -> Unit,
    onDeleteTripClick: (CurrentLoad) -> Unit,
    onViewMapClick: (CurrentLoad) -> Unit,
    liveBounceMiles: Double,
    liveLoadedMiles: Double,
    isTrainingActive: Boolean,
    flatTrainerPayRate: Double,
    showHelpOnLaunch: Boolean,
    onDismissHelpDialog: () -> Unit,
) {
    val activeTrip = pastLoads.find { it.tripState != "COMPLETED" }
    val currentTargetFriday = getPayPeriodDate(System.currentTimeMillis())
    val completedLoads = pastLoads.asSequence()
        .filter { (it.tripState == "COMPLETED") && (getPayPeriodDate(it.pickupTimestamp) == currentTargetFriday) }
        .sortedByDescending { it.pickupTimestamp }
        .toList()

    var showCompletionDialog by remember { mutableStateOf(value = false) }
    var showHistoryDetailsDialog by remember { mutableStateOf(value = false) }
    var showWeeklyBreakdownDialog by remember { mutableStateOf(value = false) } 
    var selectedTripData by remember { mutableStateOf<CurrentLoad?>(null) }
    var showActiveOptionsDialog by remember { mutableStateOf(value = false) }
    var showStatementHistoryDialog by remember { mutableStateOf(value = false) }
    var selectedWeekFriday by remember { mutableStateOf(value = null as LocalDate?) }
    var showDeleteConfirmation by remember { mutableStateOf(value = false) }
    var tripToDelete by remember { mutableStateOf(value = null as CurrentLoad?) }
    var showProUpgradeDialog by remember { mutableStateOf(value = false) }
    var showUndoPromptDialog by remember { mutableStateOf(false) }
    var showMileageEditDialog by remember { mutableStateOf(false) }
    var mileageEditInput by remember { mutableStateOf("") }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(all = 16.dp),
                verticalArrangement = Arrangement.spacedBy(space = 16.dp)
            ) {
                item {
                    Text("Load Tracker Pro", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Current Payroll Week Running Totals",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showWeeklyBreakdownDialog = true },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(all = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(space = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Current Week's Pay:", color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(
                                    "$${String.format(Locale.US, "%.2f", summary.weeklyPay)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
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
                                Text("Miles Driven This Week:", color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("${summary.weeklyMilesDriven.toInt()} mi", color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Current Out-of-Route:", color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(
                                    "${summary.weeklyOutOfRoute.toInt()} mi (${
                                        String.format(
                                            Locale.US,
                                            "%.1f",
                                            summary.weeklyOorPercentage
                                        )
                                    }%)",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                if (activeTrip != null) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showActiveOptionsDialog = true },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                        ) {
                            val label = when (activeTrip.tripState) {
                                "ACTIVE_BOUNCE" -> "En Route to: ${activeTrip.shipperName ?: "Shipper"}"
                                "ACTIVE_SHIPPER" -> "Arrived at: ${activeTrip.shipperName ?: "Shipper"}"
                                "ACTIVE_LOADED" -> "En Route to: ${activeTrip.consigneeName ?: "Consignee"}"
                                "ACTIVE_CONSIGNEE" -> "Arrived at: ${activeTrip.consigneeName ?: "Consignee"}"
                                "PAUSED_AT_HOME" -> "🏠 PARKED AT HOME BASE"
                                else -> "Active Journey"
                            }

                            val isAtHome = activeTrip.tripState == "PAUSED_AT_HOME"

                            val actualBounce = if (liveBounceMiles > 0.0) maxOf(liveBounceMiles, activeTrip.bounceMilesEnd) else activeTrip.bounceMilesEnd
                            val bounceOorPct = if (activeTrip.isGoingHome || activeTrip.dispatchedBounceMiles <= 0.0) {
                                0.0
                            } else {
                                val extraBounce = maxOf(0.0, actualBounce - activeTrip.dispatchedBounceMiles)
                                (extraBounce / activeTrip.dispatchedBounceMiles) * 100.0
                            }

                            val actualLoaded = if (liveLoadedMiles > 0.0) maxOf(liveLoadedMiles, activeTrip.loadedMilesEnd) else activeTrip.loadedMilesEnd
                            val loadedOorPct = if (activeTrip.isGoingHome || activeTrip.dispatchedLoadedMiles <= 0.0) {
                                0.0
                            } else {
                                val extraLoaded = maxOf(0.0, actualLoaded - activeTrip.dispatchedLoadedMiles)
                                (extraLoaded / activeTrip.dispatchedLoadedMiles) * 100.0
                            }

                            Column(
                                modifier = Modifier
                                    .padding(all = 16.dp)
                                    .graphicsLayer(alpha = if (isAtHome) 0.6f else 1.0f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "⚠️ ACTIVE TRIP: PRO #${activeTrip.proNumber}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Tracked Bounce", color = MaterialTheme.colorScheme.onTertiaryContainer)
                                        Text(
                                            "${String.format(Locale.US, "%.1f", liveBounceMiles)} mi (${String.format(Locale.US, "%.1f", bounceOorPct)}%)",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (bounceOorPct > 0.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Tracked Loaded", color = MaterialTheme.colorScheme.onTertiaryContainer)
                                        Text(
                                            "${String.format(Locale.US, "%.1f", liveLoadedMiles)} mi (${String.format(Locale.US, "%.1f", loadedOorPct)}%)",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (loadedOorPct > 0.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f))

                                val (targetLat, targetLong, targetName) = when (activeTrip.tripState) {
                                    "ACTIVE_BOUNCE", "ACTIVE_SHIPPER" -> Triple(activeTrip.shipperLat, activeTrip.shipperLong, activeTrip.shipperName ?: "Shipper")
                                    else -> Triple(activeTrip.consigneeLat, activeTrip.consigneeLong, activeTrip.consigneeName ?: "Consignee")
                                }

                                if (targetLat != null && targetLong != null) {
                                    val context = LocalContext.current
                                    Button(
                                        onClick = { launchNavigationIntent(context, targetLat, targetLong, targetName) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                    ) {
                                        Text("🧭 Launch Navigation ($targetName)")
                                    }
                                }

                                if (activeTrip.tripState != "ACTIVE_BOUNCE" && activeTrip.tripState != "PAUSED_AT_HOME") {
                                    OutlinedButton(
                                        onClick = {
                                            when (activeTrip.tripState) {
                                                "ACTIVE_LOADED", "ACTIVE_CONSIGNEE" -> {
                                                    TrackingService.activeSegment = "Paused"
                                                    TrackingService.targetLat = activeTrip.shipperLat
                                                    TrackingService.targetLong = activeTrip.shipperLong
                                                    TrackingService.targetName = activeTrip.shipperName ?: "Shipper"
                                                    TrackingService.isGeofenceActive = false
                                                    onUpdateTripClick(activeTrip.copy(tripState = "ACTIVE_SHIPPER"))
                                                    showUndoPromptDialog = true
                                                }
                                                "ACTIVE_SHIPPER" -> {
                                                    TrackingService.activeSegment = "Bounce"
                                                    TrackingService.targetLat = activeTrip.shipperLat
                                                    TrackingService.targetLong = activeTrip.shipperLong
                                                    TrackingService.targetName = activeTrip.shipperName ?: "Shipper"
                                                    TrackingService.isGeofenceActive = true
                                                    onUpdateTripClick(activeTrip.copy(tripState = "ACTIVE_BOUNCE", dockArrivalTime = null))
                                                    showUndoPromptDialog = true
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                    ) {
                                        Text("↩️ Undo Step (Go Back)")
                                    }
                                }

                                when (activeTrip.tripState) {
                                    "PAUSED_AT_HOME" -> {
                                        Button(
                                            onClick = {
                                                TrackingService.activeSegment = "Loaded"
                                                TrackingService.targetLat = activeTrip.consigneeLat
                                                TrackingService.targetLong = activeTrip.consigneeLong
                                                TrackingService.targetName = activeTrip.consigneeName ?: "Consignee"
                                                TrackingService.isGeofenceActive = activeTrip.consigneeLat != null
                                                onUpdateTripClick(activeTrip.copy(tripState = "ACTIVE_LOADED"))
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Departing Home (Resume Tracking)")
                                        }
                                    }
                                    "ACTIVE_BOUNCE" -> {
                                        Button(
                                            onClick = {
                                                TrackingService.activeSegment = "Paused"
                                                onUpdateTripClick(
                                                    activeTrip.copy(
                                                        tripState = "ACTIVE_SHIPPER",
                                                        bounceMilesEnd = liveBounceMiles,
                                                        dockArrivalTime = System.currentTimeMillis()
                                                    )
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) { Text("Arrive at Shipper") }
                                    }

                                    "ACTIVE_SHIPPER" -> {
                                        val arrivalTs = activeTrip.dockArrivalTime ?: System.currentTimeMillis()
                                        val apptTs = activeTrip.pickupApptTimestamp
                                        val apptType = activeTrip.pickupApptType
                                        val apptText = activeTrip.pickupApptText

                                        val isBeforeWindow = apptType.equals("BEFORE", ignoreCase = true) || apptType.equals("B", ignoreCase = true) || apptType?.contains("before", ignoreCase = true) == true
                                        val clockStartTs = if (isBeforeWindow || apptTs == null) {
                                            arrivalTs
                                        } else {
                                            if (arrivalTs < apptTs) apptTs else arrivalTs
                                        }

                                        val now = System.currentTimeMillis()
                                        val totalDockMins = maxOf(0L, (now - arrivalTs) / 60000L)
                                        val clockMins = maxOf(0L, (now - clockStartTs) / 60000L)

                                        val totalDockStr = "${totalDockMins / 60}h ${totalDockMins % 60}m"
                                        val is1Point5HrAlert = clockMins >= 90L
                                        val billableMins = maxOf(0L, clockMins - 120L)

                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("⏱️ Facility Dock Time:", style = MaterialTheme.typography.titleSmall)
                                                    Text(totalDockStr, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                }

                                                if (!apptText.isNullOrBlank()) {
                                                    Text("Appt Schedule: $apptText", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                                }

                                                if (is1Point5HrAlert && billableMins == 0L) {
                                                    Text(
                                                        "⚠️ 1.5 Hours Elapsed: Contact Detention Dispatch!",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.error,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                } else if (billableMins > 0L) {
                                                    val detHrs = billableMins / 60
                                                    val detMins = billableMins % 60
                                                    Text(
                                                        "🚨 Detention Accruing: ${detHrs}h ${detMins}m",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.error,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                } else {
                                                    val remainingFree = 120L - clockMins
                                                    Text(
                                                        "Free Time: ${remainingFree}m remaining",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                mileageEditInput = String.format(Locale.US, "%.1f", if (liveBounceMiles > 0) liveBounceMiles else activeTrip.bounceMilesEnd)
                                                showMileageEditDialog = true
                                            },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                        ) {
                                            Text("✏️ Adjust Bounce Miles")
                                        }

                                        Button(
                                            onClick = {
                                                TrackingService.activeSegment = "Loaded"
                                                TrackingService.targetLat = activeTrip.consigneeLat
                                                TrackingService.targetLong = activeTrip.consigneeLong
                                                TrackingService.targetName = activeTrip.consigneeName ?: "Consignee"
                                                TrackingService.isGeofenceActive = activeTrip.consigneeLat != null
                                                onUpdateTripClick(activeTrip.copy(tripState = "ACTIVE_LOADED", dockArrivalTime = null))
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) { Text("Depart Shipper (Loaded)") }
                                    }

                                    "ACTIVE_LOADED", "ACTIVE_CONSIGNEE" -> {
                                        if (activeTrip.tripState == "ACTIVE_CONSIGNEE") {
                                            val arrivalTs = activeTrip.dockArrivalTime ?: System.currentTimeMillis()
                                            val apptTs = activeTrip.consigneeApptTimestamp
                                            val apptType = activeTrip.consigneeApptType
                                            val apptText = activeTrip.consigneeApptText

                                            val isBeforeWindow = apptType.equals("BEFORE", ignoreCase = true) || apptType.equals("B", ignoreCase = true) || apptType?.contains("before", ignoreCase = true) == true
                                            val clockStartTs = if (isBeforeWindow || apptTs == null) {
                                                arrivalTs
                                            } else {
                                                if (arrivalTs < apptTs) apptTs else arrivalTs
                                            }

                                            val now = System.currentTimeMillis()
                                            val totalDockMins = maxOf(0L, (now - arrivalTs) / 60000L)
                                            val clockMins = maxOf(0L, (now - clockStartTs) / 60000L)

                                            val totalDockStr = "${totalDockMins / 60}h ${totalDockMins % 60}m"
                                            val is1Point5HrAlert = clockMins >= 90L
                                            val billableMins = maxOf(0L, clockMins - 120L)

                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text("⏱️ Facility Dock Time:", style = MaterialTheme.typography.titleSmall)
                                                        Text(totalDockStr, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                    }

                                                    if (!apptText.isNullOrBlank()) {
                                                        Text("Appt Schedule: $apptText", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                                    }

                                                    if (is1Point5HrAlert && billableMins == 0L) {
                                                        Text(
                                                            "⚠️ 1.5 Hours Elapsed: Contact Detention Dispatch!",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = MaterialTheme.colorScheme.error,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    } else if (billableMins > 0L) {
                                                        val detHrs = billableMins / 60
                                                        val detMins = billableMins % 60
                                                        Text(
                                                            "🚨 Detention Accruing: ${detHrs}h ${detMins}m",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = MaterialTheme.colorScheme.error,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    } else {
                                                        val remainingFree = 120L - clockMins
                                                        Text(
                                                            "Free Time: ${remainingFree}m remaining",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        if (activeTrip.tripState == "ACTIVE_CONSIGNEE") {
                                            OutlinedButton(
                                                onClick = {
                                                    mileageEditInput = String.format(Locale.US, "%.1f", if (liveLoadedMiles > 0) liveLoadedMiles else activeTrip.loadedMilesEnd)
                                                    showMileageEditDialog = true
                                                },
                                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                            ) {
                                                Text("✏️ Adjust Loaded Miles")
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                TrackingService.isGeofenceActive = false
                                                selectedTripData = activeTrip.copy(
                                                    tripState = "COMPLETED",
                                                    loadedMilesEnd = liveLoadedMiles,
                                                    deliveryTimestamp = System.currentTimeMillis()
                                                )
                                                showCompletionDialog = true
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                if (activeTrip.tripState == "ACTIVE_CONSIGNEE") "Complete Load & File Settlement" else "Arrive at Consignee"
                                            )
                                        }
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
                        "Current Week Load Logs (${completedLoads.size})",
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
                                .padding(all = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No completed logs recorded for this pay week.",
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                } else {
                    items(items = completedLoads) { load ->
                        val dateLabel = remember(key1 = load.pickupTimestamp) {
                            val instant = Instant.ofEpochMilli(load.pickupTimestamp)
                            val formatter =
                                DateTimeFormatter.ofPattern("MM/dd/yyyy")
                            instant.atZone(ZoneOffset.UTC).toLocalDate().format(formatter)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        selectedTripData = load
                                        showCompletionDialog = false
                                        showWeeklyBreakdownDialog = false
                                        showHistoryDetailsDialog = true
                                    },
                                    onLongClick = {
                                        tripToDelete = load
                                        showDeleteConfirmation = true
                                    }
                                )
                        ) {
                            Column(modifier = Modifier.padding(all = 16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "PRO #: ${load.proNumber}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = dateLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                    if ((load.shipperName != null) || (load.consigneeName != null)) {
                                        Text(
                                            text = "${load.shipperName ?: "?"} -> ${load.consigneeName ?: "?"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary,
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
                                        "L" -> "8' Drop"
                                        "S" -> "4' Drop"
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
                                Text("$${String.format(Locale.US, "%.2f", baseGross)}")
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Your Pay Split (${splitVal.toInt()}%):")
                                Text("$${String.format(Locale.US, "%.2f", baseCut)}")
                            }
                            if (tarp > 0.0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Tarp Pay Addon:")
                                    Text("+$${String.format(Locale.US, "%.2f", tarp)}")
                                }
                            }
                            if (dhBonus > 0.0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Long Deadhead Bonus:")
                                    Text("+$${String.format(Locale.US, "%.2f", dhBonus)}")
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Net Take-Home Pay:", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "$${String.format(Locale.US, "%.2f", finalNet)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
            confirmButton = {
                Button(onClick = {
                    onUpdateTripClick(trip)
                    showCompletionDialog = false
                    selectedTripData = null
                }) { Text("Confirm & File Log") }
            },
                )
            }

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
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(space = 8.dp)
                        ) {
                            Text(
                                "Shipper & Consignee Details",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Shipper:")
                                Text(trip.shipperName?.trim()?.ifBlank { "Not Specified" } ?: "Not Specified")
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Consignee:")
                                Text(trip.consigneeName?.trim()?.ifBlank { "Not Specified" } ?: "Not Specified")
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

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
                                Text("$${String.format(Locale.US, "%.2f", baseGross)}")
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Your Percentage Share (${splitVal.toInt()}%):")
                                Text("$${String.format(Locale.US, "%.2f", baseCut)}")
                            }
                            if (tarp > 0.0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Tarp Allowance:")
                                    Text("+$${String.format(Locale.US, "%.2f", tarp)}")
                                }
                            }
                            if (dhBonus > 0.0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Deadhead Bonus Pay:")
                                    Text("+$${String.format(Locale.US, "%.2f", dhBonus)}")
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Net Statement Earnings:", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "$${String.format(Locale.US, "%.2f", finalNet)}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Text(
                                "Miles Breakdown & Performance",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Bounce Miles:")
                                Text("Actual ${actualBounceRun.toInt()} mi / Disp ${trip.dispatchedBounceMiles.toInt()} mi")
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Loaded Miles:")
                                Text("Actual ${actualLoadedRun.toInt()} mi / Disp ${trip.dispatchedLoadedMiles.toInt()} mi")
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Route Miles:")
                                Text("Actual ${totalActualTripDriven.toInt()} mi / Disp ${totalDispatchedTripExpected.toInt()} mi")
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
                                Text(trip.tripNotes, style = MaterialTheme.typography.bodyMedium)
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            if (isProUser) {
                                Button(
                                    onClick = { 
                                        showHistoryDetailsDialog = false
                                        onViewMapClick(trip) 
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("View Route Heatmap")
                                }
                            } else {
                                Button(
                                    onClick = {
                                        showHistoryDetailsDialog = false
                                        showProUpgradeDialog = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Text("View Route Heatmap (Pro)")
                                }
                            }

                            Button(
                                onClick = {
                                    showHistoryDetailsDialog = false
                                    if (isProUser) {
                                        onEditTripClick(trip)
                                    } else {
                                        showProUpgradeDialog = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isProUser) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isProUser) "✏️ Edit Load Details" else "✏️ Edit Load Details (Pro)")
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

    if (showWeeklyBreakdownDialog) {
        AlertDialog(
            onDismissRequest = { showWeeklyBreakdownDialog = false },
            title = { Text("Weekly Earnings Breakdown") },
            text = {
                val currentTargetFriday = getPayPeriodDate(System.currentTimeMillis())

                val weeklyTrips = pastLoads.filter { load ->
                    getPayPeriodDate(load.pickupTimestamp) == currentTargetFriday && load.tripState == "COMPLETED"
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(space = 10.dp),
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
                                    modifier = Modifier.padding(all = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(space = 4.dp)
                                ) {
                                    Text(
                                        "PRO #: ${trip.proNumber}",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Truck Gross Pay:", style = MaterialTheme.typography.bodySmall)
                                        Text("$${String.format(Locale.US, "%.2f", trip.loadPay)}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Your Split (${trip.percentageRate.toInt()}%):", style = MaterialTheme.typography.bodySmall)
                                        Text("$${String.format(Locale.US, "%.2f", baseDriverCut)}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (tarpAddon > 0.0 || bounceBonus > 0.0) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Extras (Tarp/Bounce):", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                            Text("+$${String.format(Locale.US, "%.2f", tarpAddon + bounceBonus)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Total Load Net:", style = MaterialTheme.typography.bodyMedium)
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Training:")
                            Text(if (isTrainingActive) "Yes" else "No", style = MaterialTheme.typography.bodyMedium)
                        }
                        if (isTrainingActive) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Flat Trainer Premium Addon:")
                                Text("+$${String.format(Locale.US, "%.2f", flatTrainerPayRate)}", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Combined Statement Total:", style = MaterialTheme.typography.titleSmall)
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
        val baseGross = activeTrip.loadPay
        val splitVal = activeTrip.percentageRate
        val baseCut = baseGross * (splitVal / 100.0)

        AlertDialog(
            onDismissRequest = { showActiveOptionsDialog = false },
            title = { Text("Active Trip Details - PRO #${activeTrip.proNumber}") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(space = 8.dp)
                ) {
                    Text(
                        "Shipper & Consignee Details",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Shipper:")
                        Text(activeTrip.shipperName?.trim()?.ifBlank { "Not Specified" } ?: "Not Specified")
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Consignee:")
                        Text(activeTrip.consigneeName?.trim()?.ifBlank { "Not Specified" } ?: "Not Specified")
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        "Revenue Details",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Gross Truck Revenue:")
                        Text("$${String.format(Locale.US, "%.2f", baseGross)}")
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Your Pay Split (${splitVal.toInt()}%):")
                        Text("$${String.format(Locale.US, "%.2f", baseCut)}")
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        "Miles Breakdown",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Bounce Miles:")
                        Text("Tracked ${String.format(Locale.US, "%.1f", liveBounceMiles)} mi / Disp ${activeTrip.dispatchedBounceMiles.toInt()} mi")
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Loaded Miles:")
                        Text("Tracked ${String.format(Locale.US, "%.1f", liveLoadedMiles)} mi / Disp ${activeTrip.dispatchedLoadedMiles.toInt()} mi")
                    }

                    if (!activeTrip.tripNotes.isNullOrBlank()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            "Trip Notes",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(activeTrip.tripNotes, style = MaterialTheme.typography.bodyMedium)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

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
        val grouped = remember(key1 = pastLoads) {
            pastLoads.asSequence().filter { it.tripState == "COMPLETED" }
                .groupBy { load -> getPayPeriodDate(load.pickupTimestamp) }
                .toSortedMap(reverseOrder())
        }

        AlertDialog(
            onDismissRequest = { showStatementHistoryDialog = false },
            title = { Text("Historical Payroll Statements") },
            text = {
                Box(modifier = Modifier.heightIn(max = 500.dp)) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                        if (grouped.isEmpty()) {
                            item { Text("No historical records found.") }
                        } else {
                            grouped.forEach { (friday, loads) ->
                                item {
                                    val totalWeekGross = loads.sumOf { it.loadPay }
                                    val weekTrainerAddon = loads.maxOfOrNull { it.trainerPayRate }?.takeIf { it > 0.0 }
                                        ?: if (loads.any { it.isTrainingWeek }) flatTrainerPayRate else 0.0
                                    val totalWeekPay = loads.sumOf { load ->
                                        val base = load.loadPay * (load.percentageRate / 100.0)
                                        var tarp = when (load.tarpType) {
                                            "L" -> 50.0
                                            "S" -> 30.0
                                            else -> 0.0
                                        }
                                        if (load.isPreTarped) tarp /= 2.0
                                        val bonus = if (load.dispatchedBounceMiles >= 150.0) load.dispatchedBounceMiles * 0.20 else 0.0
                                        base + tarp + bonus
                                    } + weekTrainerAddon
                                    Card(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            selectedWeekFriday = friday
                                        },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(all = 16.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("Week Ending:", style = MaterialTheme.typography.labelSmall)
                                                Text(friday.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")), style = MaterialTheme.typography.titleMedium)
                                                Text("${loads.size} load(s) • Gross: $${String.format(Locale.US, "%.2f", totalWeekGross)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("Est Net Pay:", style = MaterialTheme.typography.labelSmall)
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
            getPayPeriodDate(load.pickupTimestamp) == weekFriday && load.tripState == "COMPLETED"
        }

        val totalGross = weeklyTrips.sumOf { it.loadPay }
        val totalDriverBase = weeklyTrips.sumOf { it.loadPay * (it.percentageRate / 100.0) }
        val totalTarpPay = weeklyTrips.sumOf { load ->
            var tarp = when (load.tarpType) {
                "L" -> 50.0
                "S" -> 30.0
                else -> 0.0
            }
            if (load.isPreTarped) tarp /= 2.0
            tarp
        }
        val totalBouncePay = weeklyTrips.sumOf { load ->
            if (load.dispatchedBounceMiles >= 150.0) load.dispatchedBounceMiles * 0.20 else 0.0
        }
        val totalTrainerPay = weeklyTrips.maxOfOrNull { it.trainerPayRate }?.takeIf { it > 0.0 }
            ?: if (weeklyTrips.any { it.isTrainingWeek }) flatTrainerPayRate else 0.0
        val totalNetPay = totalDriverBase + totalTarpPay + totalBouncePay + totalTrainerPay

        AlertDialog(
            onDismissRequest = { selectedWeekFriday = null },
            title = { Text("Statement: ${weekFriday.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))}") },
            text = {
                Box(modifier = Modifier.heightIn(max = 500.dp)) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(space = 10.dp)) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Column(
                                    modifier = Modifier.padding(all = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(space = 6.dp)
                                ) {
                                    Text(
                                        "Week Summary Breakdown",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Total Truck Gross:", style = MaterialTheme.typography.bodySmall)
                                        Text("$${String.format(Locale.US, "%.2f", totalGross)}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Driver Load Cut:", style = MaterialTheme.typography.bodySmall)
                                        Text("$${String.format(Locale.US, "%.2f", totalDriverBase)}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Tarp Pay:", style = MaterialTheme.typography.bodySmall)
                                        Text("$${String.format(Locale.US, "%.2f", totalTarpPay)}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Deadhead Pay:", style = MaterialTheme.typography.bodySmall)
                                        Text("$${String.format(Locale.US, "%.2f", totalBouncePay)}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (totalTrainerPay > 0.0) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Trainer Pay:", style = MaterialTheme.typography.bodySmall)
                                            Text("$${String.format(Locale.US, "%.2f", totalTrainerPay)}", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Total Net Pay (Est):", style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "$${String.format(Locale.US, "%.2f", totalNetPay)}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                "Individual Trip Logs (${weeklyTrips.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        items(weeklyTrips) { trip ->
                            var tarp = when (trip.tarpType) {
                                "L" -> 50.0
                                "S" -> 30.0
                                else -> 0.0
                            }
                            if (trip.isPreTarped) tarp /= 2.0
                            val bounce = if (trip.dispatchedBounceMiles >= 150.0) trip.dispatchedBounceMiles * 0.20 else 0.0
                            val baseCut = trip.loadPay * (trip.percentageRate / 100.0)
                            val tripNet = baseCut + tarp + bounce

                            Card(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    selectedTripData = trip
                                    showHistoryDetailsDialog = true
                                }
                            ) {
                                Column(modifier = Modifier.padding(all = 12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("PRO #${trip.proNumber}", style = MaterialTheme.typography.titleSmall)
                                        Text("$${String.format(Locale.US, "%.2f", tripNet)}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Gross: $${String.format(Locale.US, "%.2f", trip.loadPay)} (${trip.percentageRate.toInt()}%)", style = MaterialTheme.typography.bodySmall)
                                        Text("${trip.dispatchedLoadedMiles.toInt()} mi Loaded", style = MaterialTheme.typography.bodySmall)
                                    }
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

    if (showDeleteConfirmation && tripToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Record?") },
            text = { Text("Are you sure you want to permanently delete PRO #${tripToDelete!!.proNumber}? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTripClick(tripToDelete!!)
                        showDeleteConfirmation = false
                        tripToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete Forever") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancel") }
            }
        )
    }

    if (showProUpgradeDialog) {
        ProPaywallDialog(
            onUnlockClick = {
                showProUpgradeDialog = false
            },
            onDismiss = { showProUpgradeDialog = false }
        )
    }

    if (showUndoPromptDialog) {
        AlertDialog(
            onDismissRequest = { showUndoPromptDialog = false },
            title = { Text("↩️ Step Reverted Successfully") },
            text = { Text("Do you need to adjust or correct your tracked mileage for this leg?") },
            confirmButton = {
                Button(onClick = {
                    showUndoPromptDialog = false
                    mileageEditInput = ""
                    showMileageEditDialog = true
                }) { Text("Yes, Adjust Miles") }
            },
            dismissButton = {
                TextButton(onClick = { showUndoPromptDialog = false }) { Text("No, Keep Mileage As-Is") }
            }
        )
    }

    if (showMileageEditDialog) {
        var correctedMilesStr by remember { mutableStateOf(mileageEditInput) }
        AlertDialog(
            onDismissRequest = { showMileageEditDialog = false },
            title = { Text("✏️ Correct Leg Mileage") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter the correct total miles for this leg:")
                    OutlinedTextField(
                        value = correctedMilesStr,
                        onValueChange = { correctedMilesStr = it },
                        label = { Text("Corrected Miles") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val newMiles = correctedMilesStr.toDoubleOrNull()
                    if (newMiles != null && activeTrip != null) {
                        if (activeTrip.tripState == "ACTIVE_SHIPPER" || activeTrip.tripState == "ACTIVE_BOUNCE") {
                            TrackingService.totalBounceMilesTracked.value = newMiles
                            onUpdateTripClick(activeTrip.copy(bounceMilesEnd = newMiles))
                        } else {
                            TrackingService.totalLoadedMilesTracked.value = newMiles
                            onUpdateTripClick(activeTrip.copy(loadedMilesEnd = newMiles))
                        }
                    }
                    showMileageEditDialog = false
                }) { Text("Save Corrected Miles") }
            },
            dismissButton = {
                TextButton(onClick = { showMileageEditDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showHelpOnLaunch) {
        val context = LocalContext.current
        val helpTextString = remember {
            try {
                context.assets.open("help_guide.md").bufferedReader().use { it.readText() }
            } catch (_: Exception) {
                "# Error\nCould not locate your `help_guide.md` asset file."
            }
        }

        AlertDialog(
            onDismissRequest = onDismissHelpDialog,
            title = { Text("App Reference Manual") },
            text = {
                Box(modifier = Modifier.heightIn(max = 400.dp)) {
                    LazyColumn {
                        item {
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

private fun launchNavigationIntent(context: Context, lat: Double?, lng: Double?, facilityName: String?) {
    if (lat != null && lng != null) {
        val label = facilityName ?: "Facility Destination"
        val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})")
        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
        val chooser = Intent.createChooser(mapIntent, "Navigate with Trucker Path or Maps")
        try {
            context.startActivity(chooser)
        } catch (_: Exception) {
            Toast.makeText(context, "No navigation app found on device.", Toast.LENGTH_LONG).show()
        }
    } else {
        Toast.makeText(context, "GPS coordinates not available for this facility.", Toast.LENGTH_SHORT).show()
    }
} 
