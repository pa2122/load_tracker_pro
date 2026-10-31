package com.loadtracker.pro

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.jeziellago.compose.markdowntext.MarkdownText
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    summary: WeekSummary,
    pastLoads: List<CurrentLoad>,
    isProUser: Boolean,
    onAddNewLoadClick: () -> Unit,
    onAddHistoricalLoadClick: (() -> Unit)? = null,
    onUpdateTripClick: (CurrentLoad) -> Unit,
    onEditTripClick: (CurrentLoad) -> Unit,
    onDeleteTripClick: (CurrentLoad) -> Unit,
    onViewMapClick: (CurrentLoad) -> Unit,
    liveBounceMiles: Double,
    liveLoadedMiles: Double,
    isTrainingActive: Boolean,
    flatTrainerPayRate: Double,
    extraStopRate: Double,
    showHelpOnLaunch: Boolean,
    onDismissHelpDialog: () -> Unit,
    onEditDeliveryDate: (CurrentLoad, Long) -> Unit = { _, _ -> },
) {
    val activeTrip = pastLoads.find { it.tripState != "COMPLETED" }
    val ctx = LocalContext.current
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(activeTrip?.tripState, activeTrip?.dockArrivalTime) {
        if (activeTrip?.tripState == "ACTIVE_SHIPPER" || activeTrip?.tripState == "ACTIVE_CONSIGNEE") {
            while (true) {
                nowMs = System.currentTimeMillis()
                delay(60_000L)
            }
        }
    }
    val currentPayrollWeekStart = getPayrollWeekStartDate(System.currentTimeMillis())
    val completedLoads = pastLoads
        .filter { it.payrollWeekStartDate() == currentPayrollWeekStart }
        .sortedByNewestDelivery()

    var showCompletionDialog by remember { mutableStateOf(value = false) }
    var showMissingAddressDialog by remember { mutableStateOf(value = false) }
    var showHistoryDetailsDialog by remember { mutableStateOf(value = false) }
    var showWeeklyBreakdownDialog by remember { mutableStateOf(value = false) } 
    var selectedTripData by remember { mutableStateOf<CurrentLoad?>(null) }
    var showActiveOptionsDialog by remember { mutableStateOf(value = false) }
    var showStatementHistoryDialog by remember { mutableStateOf(value = false) }
    var selectedWeekStart by remember { mutableStateOf(value = null as LocalDate?) }
    var showDeleteConfirmation by remember { mutableStateOf(value = false) }
    var tripToDelete by remember { mutableStateOf(value = null as CurrentLoad?) }
    var showProUpgradeDialog by remember { mutableStateOf(value = false) }
    var showUndoPromptDialog by remember { mutableStateOf(false) }
    var showMileageEditDialog by remember { mutableStateOf(false) }
    var mileageEditInput by remember { mutableStateOf("") }
    var mileageEditError by remember { mutableStateOf<Int?>(null) }

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
                            val activeStops = remember(activeTrip.stopOffsData) {
                                LoadStopCodec.decode(activeTrip.stopOffsData)
                            }
                            val arrivedStop = activeStops.getOrNull(activeTrip.currentStopIndex - 1)
                            val nextStop = activeStops.getOrNull(activeTrip.currentStopIndex)
                            val label = when (activeTrip.tripState) {
                                "ACTIVE_BOUNCE" -> "En Route to: ${activeTrip.shipperName ?: "Shipper"}"
                                "ACTIVE_SHIPPER" -> "Arrived at: ${activeTrip.shipperName ?: "Shipper"}"
                                "ACTIVE_LOADED" -> "En Route to: ${nextStop?.name ?: activeTrip.consigneeName ?: "Consignee"}"
                                "ACTIVE_STOP" -> "Arrived at: ${arrivedStop?.name ?: "Stop Off"}"
                                "ACTIVE_CONSIGNEE" -> "Arrived at: ${activeTrip.consigneeName ?: "Consignee"}"
                                "PAUSED_AT_HOME" -> "🏠 PARKED AT HOME BASE"
                                else -> "Active Journey"
                            }

                            val isAtHome = activeTrip.tripState == "PAUSED_AT_HOME"

                            val displayBounceMiles = maxOf(liveBounceMiles, activeTrip.bounceMilesEnd)
                            val bounceOorPct = if (activeTrip.isGoingHome || activeTrip.dispatchedBounceMiles <= 0.0) {
                                0.0
                            } else {
                                val extraBounce = maxOf(0.0, displayBounceMiles - activeTrip.dispatchedBounceMiles)
                                (extraBounce / activeTrip.dispatchedBounceMiles) * 100.0
                            }

                            val displayLoadedMiles = maxOf(liveLoadedMiles, activeTrip.loadedMilesEnd)
                            val loadedLegPlan = when (activeTrip.tripState) {
                                "ACTIVE_STOP" -> arrivedStop?.dispatchedLegMiles ?: 0.0
                                "ACTIVE_CONSIGNEE" -> activeTrip.plannedCurrentLoadedLegMiles()
                                else -> activeTrip.plannedCurrentLoadedLegMiles()
                            }
                            val loadedLegActual = when (activeTrip.tripState) {
                                "ACTIVE_STOP" -> arrivedStop?.actualLegMiles ?: 0.0
                                "ACTIVE_CONSIGNEE" ->
                                    (displayLoadedMiles - activeTrip.lastStopLoadedMiles).coerceAtLeast(0.0)
                                else -> (displayLoadedMiles - activeTrip.lastStopLoadedMiles).coerceAtLeast(0.0)
                            }
                            val loadedLegOor = (loadedLegActual - loadedLegPlan).coerceAtLeast(0.0)
                            val loadedLegOorPercent =
                                if (loadedLegPlan > 0.0) loadedLegOor / loadedLegPlan * 100.0 else 0.0
                            val loadedOorPct = if (activeTrip.isGoingHome || activeTrip.dispatchedLoadedMiles <= 0.0) {
                                0.0
                            } else {
                                val extraLoaded = maxOf(0.0, displayLoadedMiles - activeTrip.dispatchedLoadedMiles)
                                (extraLoaded / activeTrip.dispatchedLoadedMiles) * 100.0
                            }

                            Column(
                                modifier = Modifier
                                    .padding(all = 16.dp)
                                    .graphicsLayer(alpha = if (isAtHome) 0.6f else 1.0f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                var showCardMenu by remember { mutableStateOf(value = false) }
                                val dock = remember(activeTrip.tripState, activeTrip.dockArrivalTime, activeTrip.pickupApptTimestamp, activeTrip.consigneeApptTimestamp, nowMs) {
                                    when (activeTrip.tripState) {
                                        "ACTIVE_SHIPPER" -> DockTimeCalculator.calculate(
                                            arrivalTs = activeTrip.dockArrivalTime ?: nowMs,
                                            apptTs = activeTrip.pickupApptTimestamp,
                                            apptType = activeTrip.pickupApptType,
                                            apptText = activeTrip.pickupApptText,
                                            nowMs = nowMs
                                        )
                                        "ACTIVE_CONSIGNEE" -> DockTimeCalculator.calculate(
                                            arrivalTs = activeTrip.dockArrivalTime ?: nowMs,
                                            apptTs = activeTrip.consigneeApptTimestamp,
                                            apptType = activeTrip.consigneeApptType,
                                            apptText = activeTrip.consigneeApptText,
                                            nowMs = nowMs
                                        )
                                        else -> null
                                    }
                                }
                                val canUndo = activeTrip.tripState in setOf("ACTIVE_LOADED", "ACTIVE_SHIPPER", "ACTIVE_CONSIGNEE")
                                val canAdjustMiles = activeTrip.tripState == "ACTIVE_SHIPPER" || activeTrip.tripState == "ACTIVE_CONSIGNEE"
                                val hasCardMenu = canUndo || dock != null
                                val clipboardManager = LocalClipboardManager.current

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        stringResource(
                                            R.string.card_trip_header,
                                            activeTrip.proNumber,
                                            label.lines().first()
                                        ),
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    if (hasCardMenu) {
                                        Box {
                                            IconButton(onClick = { showCardMenu = true }) {
                                                Icon(
                                                    Icons.Default.MoreVert,
                                                    contentDescription = stringResource(R.string.card_more_actions),
                                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = showCardMenu,
                                                onDismissRequest = { showCardMenu = false }
                                            ) {
                                                if (dock != null) {
                                                    DropdownMenuItem(
                                                        text = { Text(stringResource(R.string.card_copy_detention)) },
                                                        onClick = {
                                                            showCardMenu = false
                                                            val facility = if (activeTrip.tripState == "ACTIVE_SHIPPER") {
                                                                activeTrip.shipperName ?: "Shipper Facility"
                                                            } else {
                                                                activeTrip.consigneeName ?: "Consignee Facility"
                                                            }
                                                            clipboardManager.setText(
                                                                AnnotatedString(
                                                                    DockTimeCalculator.detentionStatement(activeTrip.proNumber, facility, dock)
                                                                )
                                                            )
                                                            Toast.makeText(ctx, ctx.getString(R.string.detention_copied), Toast.LENGTH_SHORT).show()
                                                        }
                                                    )
                                                }
                                                if (canUndo) {
                                                    DropdownMenuItem(
                                                        text = { Text(stringResource(R.string.card_undo_step)) },
                                                        onClick = {
                                                            showCardMenu = false
                                                            when (activeTrip.tripState) {
                                                                "ACTIVE_LOADED", "ACTIVE_CONSIGNEE" -> {
                                                                    TrackingService.activeSegment = "Paused"
                                                                    TrackingService.targetLat = activeTrip.shipperLat
                                                                    TrackingService.targetLong = activeTrip.shipperLong
                                                                    TrackingService.targetName = activeTrip.shipperName ?: "Shipper"
                                                                    TrackingService.isGeofenceActive = false
                                                                    onUpdateTripClick(activeTrip.copy(tripState = "ACTIVE_SHIPPER", dockArrivalTime = null))
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
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                if (dock != null) {
                                    DockTimeSummary(dock)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                if (activeTrip.tripState == "ACTIVE_SHIPPER") {
                                    Button(
                                        onClick = {
                                            TrackingService.activeSegment = "Loaded"
                                            val destination = activeTrip.nextLoadedDestination()
                                            TrackingService.targetLat = destination?.latitude ?: activeTrip.consigneeLat
                                            TrackingService.targetLong = destination?.longitude ?: activeTrip.consigneeLong
                                            TrackingService.targetName =
                                                destination?.name ?: activeTrip.consigneeName ?: "Consignee"
                                            TrackingService.isGeofenceActive = TrackingService.targetLat != null
                                            TrackingService.departureGraceTimeMs = System.currentTimeMillis()
                                            onUpdateTripClick(activeTrip.copy(tripState = "ACTIVE_LOADED", dockArrivalTime = null))
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(stringResource(R.string.card_depart_shipper_loaded))
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        stringResource(
                                            R.string.card_miles_summary,
                                            if (displayBounceMiles > 0.0 && bounceOorPct > 0.0) {
                                                stringResource(
                                                    R.string.card_miles_value_oor,
                                                    String.format(Locale.US, "%.1f", displayBounceMiles),
                                                    String.format(Locale.US, "%.1f", bounceOorPct)
                                                )
                                            } else {
                                                stringResource(
                                                    R.string.card_miles_value,
                                                    String.format(Locale.US, "%.1f", displayBounceMiles)
                                                )
                                            },
                                            if (displayLoadedMiles > 0.0 && loadedOorPct > 0.0) {
                                                stringResource(
                                                    R.string.card_miles_value_oor,
                                                    String.format(Locale.US, "%.1f", displayLoadedMiles),
                                                    String.format(Locale.US, "%.1f", loadedOorPct)
                                                )
                                            } else {
                                                stringResource(
                                                    R.string.card_miles_value,
                                                    String.format(Locale.US, "%.1f", displayLoadedMiles)
                                                )
                                            }
                                        ),
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if ((displayBounceMiles > 0.0 && bounceOorPct > 0.0) ||
                                            (displayLoadedMiles > 0.0 && loadedOorPct > 0.0)
                                        ) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    if (canAdjustMiles) {
                                        TextButton(
                                            onClick = {
                                                val isBounce = activeTrip.tripState == "ACTIVE_SHIPPER"
                                                val live = if (isBounce) liveBounceMiles else liveLoadedMiles
                                                val saved = if (isBounce) activeTrip.bounceMilesEnd else activeTrip.loadedMilesEnd
                                                mileageEditInput = String.format(Locale.US, "%.1f", if (live > 0) live else saved)
                                                mileageEditError = null
                                                showMileageEditDialog = true
                                            }
                                        ) {
                                            Text(stringResource(R.string.card_edit_miles))
                                        }
                                    }
                                }
                                if (activeTrip.tripState.startsWith("ACTIVE_LOADED") ||
                                    activeTrip.tripState == "ACTIVE_STOP" ||
                                    activeTrip.tripState == "ACTIVE_CONSIGNEE"
                                ) {
                                    Text(
                                        "Current loaded leg: ${String.format(Locale.US, "%.1f", loadedLegActual)} / ${String.format(Locale.US, "%.1f", loadedLegPlan)} dispatched mi • " +
                                                "${String.format(Locale.US, "%.1f", loadedLegOor)} mi OOR (${String.format(Locale.US, "%.1f", loadedLegOorPercent)}%)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (loadedLegOor > 0.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    if (arrivedStop != null && activeTrip.tripState == "ACTIVE_STOP") {
                                        Text(
                                            "Stop Off ${activeTrip.currentStopIndex} leg recorded: ${String.format(Locale.US, "%.1f", arrivedStop.actualLegMiles ?: 0.0)} / ${String.format(Locale.US, "%.1f", arrivedStop.dispatchedLegMiles)} mi",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    activeStops.forEachIndexed { index, stop ->
                                        val actual = stop.actualLegMiles
                                        val stopStatus = when {
                                            actual != null ->
                                                "${String.format(Locale.US, "%.1f", actual)} / ${String.format(Locale.US, "%.1f", stop.dispatchedLegMiles)} mi • ${String.format(Locale.US, "%.1f", (actual - stop.dispatchedLegMiles).coerceAtLeast(0.0))} mi OOR"
                                            activeTrip.tripState == "ACTIVE_LOADED" &&
                                                    index == activeTrip.currentStopIndex -> "Currently tracking"
                                            else -> "Upcoming"
                                        }
                                        Text(
                                            "Stop Off ${index + 1}: ${stop.name} — $stopStatus",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (actual != null &&
                                                actual > stop.dispatchedLegMiles
                                            ) MaterialTheme.colorScheme.error
                                            else MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                    if (activeTrip.tripState == "ACTIVE_CONSIGNEE") {
                                        Text(
                                            "Final consignee leg: ${String.format(Locale.US, "%.1f", loadedLegActual)} / ${String.format(Locale.US, "%.1f", loadedLegPlan)} mi • ${String.format(Locale.US, "%.1f", loadedLegOor)} mi OOR",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (loadedLegOor > 0.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(4.dp))

                                if (activeTrip.isConsigneeAddressPending) {
                                    Text(
                                        "⚠️ Delivery address needed",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    OutlinedButton(
                                        onClick = { onEditTripClick(activeTrip) },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                    ) { Text("Add Delivery Address") }
                                }

                                val (targetLat, targetLong, targetName) = when (activeTrip.tripState) {
                                    "ACTIVE_BOUNCE", "ACTIVE_SHIPPER" ->
                                        Triple(activeTrip.shipperLat, activeTrip.shipperLong, activeTrip.shipperName ?: "Shipper")
                                    "ACTIVE_STOP" ->
                                        Triple(arrivedStop?.latitude, arrivedStop?.longitude, arrivedStop?.name ?: "Stop Off")
                                    else -> Triple(
                                        nextStop?.latitude ?: activeTrip.consigneeLat,
                                        nextStop?.longitude ?: activeTrip.consigneeLong,
                                        nextStop?.name ?: activeTrip.consigneeName ?: "Consignee"
                                    )
                                }

                                val hasArrived = activeTrip.tripState in setOf("ACTIVE_SHIPPER", "ACTIVE_STOP", "ACTIVE_CONSIGNEE")
                                if (targetLat != null && targetLong != null && !hasArrived) {
                                    val context = LocalContext.current
                                    Button(
                                        onClick = { launchNavigationIntent(context, targetLat, targetLong, targetName) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                    ) {
                                        Text(stringResource(R.string.card_navigate_to, targetName.lines().first()))
                                    }
                                }

                                val phoneMatchResult = extractPhoneNumber(activeTrip.tripNotes ?: "")
                                if (phoneMatchResult != null) {
                                    val (dialableNumber, displayString) = phoneMatchResult
                                    OutlinedButton(
                                        onClick = {
                                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$dialableNumber"))
                                            try {
                                                ctx.startActivity(dialIntent)
                                            } catch (_: Exception) {
                                                Toast.makeText(ctx, "No dialer app found.", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                    ) {
                                        Text(stringResource(R.string.card_call_shipping_office, displayString))
                                    }
                                }

                                when (activeTrip.tripState) {
                                    "PAUSED_AT_HOME" -> {
                                        Button(
                                            onClick = {
                                                TrackingService.activeSegment = "Loaded"
                                                val destination = activeTrip.nextLoadedDestination()
                                                TrackingService.targetLat = destination?.latitude ?: activeTrip.consigneeLat
                                                TrackingService.targetLong = destination?.longitude ?: activeTrip.consigneeLong
                                                TrackingService.targetName =
                                                    destination?.name ?: activeTrip.consigneeName ?: "Consignee"
                                                TrackingService.isGeofenceActive = TrackingService.targetLat != null
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

                                    "ACTIVE_STOP" -> {
                                        val stop = activeStops.getOrNull(activeTrip.currentStopIndex - 1)
                                        val actualLeg = stop?.actualLegMiles ?: 0.0
                                        val dispatchedLeg = stop?.dispatchedLegMiles ?: 0.0
                                        val legOor = (actualLeg - dispatchedLeg).coerceAtLeast(0.0)
                                        Text(
                                            "This stop leg: ${String.format(Locale.US, "%.1f", actualLeg)} tracked / ${String.format(Locale.US, "%.1f", dispatchedLeg)} dispatched mi. OOR: ${String.format(Locale.US, "%.1f", legOor)} mi.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (legOor > 0.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        )
                                        Button(
                                            onClick = {
                                                val destination = activeTrip.nextLoadedDestination()
                                                TrackingService.activeSegment = "Loaded"
                                                TrackingService.targetLat = destination?.latitude ?: activeTrip.consigneeLat
                                                TrackingService.targetLong = destination?.longitude ?: activeTrip.consigneeLong
                                                TrackingService.targetName =
                                                    destination?.name ?: activeTrip.consigneeName ?: "Consignee"
                                                TrackingService.isGeofenceActive = TrackingService.targetLat != null
                                                TrackingService.departureGraceTimeMs = System.currentTimeMillis()
                                                onUpdateTripClick(
                                                    activeTrip.copy(
                                                        tripState = "ACTIVE_LOADED",
                                                        dockArrivalTime = null
                                                    )
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                "Depart ${stop?.name ?: "Stop Off"} → " +
                                                        (activeTrip.nextLoadedDestination()?.name
                                                            ?: activeTrip.consigneeName ?: "Consignee")
                                            )
                                        }
                                    }

                                    "ACTIVE_LOADED", "ACTIVE_CONSIGNEE" -> {
                                        Button(
                                            onClick = {
                                                if (activeTrip.tripState == "ACTIVE_LOADED") {
                                                    TrackingService.activeSegment = "Paused"
                                                    TrackingService.isGeofenceActive = false
                                                    onUpdateTripClick(
                                                        activeTrip.recordLoadedArrival(
                                                            cumulativeLoadedMiles = liveLoadedMiles,
                                                            arrivalTimestamp = System.currentTimeMillis()
                                                        )
                                                    )
                                                } else {
                                                    TrackingService.isGeofenceActive = false
                                                    selectedTripData = activeTrip.copy(
                                                        tripState = "COMPLETED",
                                                        loadedMilesEnd = liveLoadedMiles,
                                                        deliveryTimestamp = System.currentTimeMillis()
                                                    )
                                                    if (activeTrip.isConsigneeAddressPending) {
                                                        showMissingAddressDialog = true
                                                    } else {
                                                        showCompletionDialog = true
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                when {
                                                    activeTrip.tripState == "ACTIVE_CONSIGNEE" ->
                                                        "Complete Load & File Settlement"
                                                    activeTrip.tripState == "ACTIVE_LOADED" &&
                                                            activeTrip.nextLoadedDestination() != null ->
                                                        "Arrive at ${activeTrip.nextLoadedDestination()?.name}"
                                                    else -> "Arrive at Consignee"
                                                }
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showStatementHistoryDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("View Statements")
                        }
                        if (onAddHistoricalLoadClick != null) {
                            OutlinedButton(
                                onClick = onAddHistoricalLoadClick,
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Text("➕ Add Past Load")
                            }
                        }
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
                        val dateLabel = remember(key1 = load.deliveryTimestamp) {
                            val formatter =
                                DateTimeFormatter.ofPattern("MM/dd/yyyy")
                            load.deliveryTimestamp?.let { deliveryTimestamp ->
                                Instant.ofEpochMilli(deliveryTimestamp)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                                    .format(formatter)
                            }.orEmpty()
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
                                        text = "Delivered $dateLabel",
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

            if (showMissingAddressDialog && selectedTripData != null) {
                AlertDialog(
                    onDismissRequest = { showMissingAddressDialog = false },
                    title = { Text("Delivery address missing") },
                    text = { Text("This load has no delivery address. You can add it now, or complete the load without it.") },
                    confirmButton = {
                        Button(onClick = {
                            showMissingAddressDialog = false
                            activeTrip?.let(onEditTripClick)
                        }) { Text("Add Address") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showMissingAddressDialog = false
                            showCompletionDialog = true
                        }) { Text("Complete Anyway") }
                    }
                )
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
                val extraStopsPay = trip.extraStopPayCents(extraStopRate).toDollars()
                val finalNet = baseCut + tarp + dhBonus + extraStopsPay
                val completionZone = ZoneId.systemDefault()
                var deliveryDate by remember(trip.proNumber) {
                    mutableStateOf(
                        Instant.ofEpochMilli(trip.deliveryTimestamp ?: System.currentTimeMillis())
                            .atZone(completionZone).toLocalDate()
                    )
                }
                var showDeliveryDatePicker by remember { mutableStateOf(false) }
                if (showDeliveryDatePicker) {
                    DeliveryDatePickerDialog(
                        initialDate = deliveryDate,
                        minDate = Instant.ofEpochMilli(trip.pickupTimestamp).atZone(completionZone).toLocalDate(),
                        maxDate = LocalDate.now(completionZone),
                        onConfirm = { deliveryDate = it; showDeliveryDatePicker = false },
                        onDismiss = { showDeliveryDatePicker = false }
                    )
                }

                AlertDialog(
                    onDismissRequest = { },
                    title = { Text("Load Settlement Summary - PRO #${trip.proNumber}") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Delivered on:")
                                OutlinedButton(onClick = { showDeliveryDatePicker = true }) {
                                    Text(deliveryDate.format(DateTimeFormatter.ofPattern("EEE MM/dd/yyyy", Locale.US)))
                                }
                            }
                            Text(
                                "Pay week: " + getPayrollWeekStartDate(
                                    deliveryTimestampForDate(deliveryDate, trip.pickupTimestamp)
                                ).let {
                                    "${it.format(DateTimeFormatter.ofPattern("MM/dd"))} - " +
                                            it.plusDays(6).format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
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
                            if (extraStopsPay > 0.0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Extra Stop Pay (${trip.extraStopCount} × $${String.format(Locale.US, "%.2f", extraStopRate)}):")
                                    Text("+$${String.format(Locale.US, "%.2f", extraStopsPay)}")
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
                val ctx = LocalContext.current
                Button(onClick = {
                    onUpdateTripClick(
                        trip.copy(
                            deliveryTimestamp = deliveryTimestampForDate(deliveryDate, trip.pickupTimestamp)
                        )
                    )
                    try {
                        ctx.stopService(Intent(ctx, TrackingService::class.java))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
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
                val extraStopsPay = trip.extraStopPayCents(extraStopRate).toDollars()
                val finalNet = baseCut + tarp + dhBonus + extraStopsPay
                val routeStops = LoadStopCodec.decode(trip.stopOffsData)
                val finalLegPlan =
                    (trip.dispatchedLoadedMiles - routeStops.sumOf { it.dispatchedLegMiles })
                        .coerceAtLeast(0.0)
                val finalLegActual =
                    (trip.loadedMilesEnd - trip.lastStopLoadedMiles).coerceAtLeast(0.0)

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
                                "Dates",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            val dateTimeFmt = remember { DateTimeFormatter.ofPattern("EEE MM/dd/yyyy h:mm a", Locale.US) }
                            val zone = ZoneId.systemDefault()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Picked up:")
                                Text(Instant.ofEpochMilli(trip.pickupTimestamp).atZone(zone).format(dateTimeFmt))
                            }
                            trip.deliveryTimestamp?.let { delivered ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Delivered:")
                                    Text(Instant.ofEpochMilli(delivered).atZone(zone).format(dateTimeFmt))
                                }
                            }
                            trip.payrollWeekStartDate()?.let { weekStart ->
                                Text(
                                    "Pay week: ${weekStart.format(DateTimeFormatter.ofPattern("MM/dd"))} - " +
                                            weekStart.plusDays(6).format(DateTimeFormatter.ofPattern("MM/dd/yyyy")),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            if (trip.tripState == "COMPLETED") {
                                var showEditDeliveryPicker by remember { mutableStateOf(false) }
                                TextButton(onClick = { showEditDeliveryPicker = true }) {
                                    Text("Edit delivery date")
                                }
                                if (showEditDeliveryPicker) {
                                    DeliveryDatePickerDialog(
                                        initialDate = Instant.ofEpochMilli(trip.deliveryTimestamp ?: trip.pickupTimestamp)
                                            .atZone(zone).toLocalDate(),
                                        minDate = Instant.ofEpochMilli(trip.pickupTimestamp).atZone(zone).toLocalDate(),
                                        maxDate = LocalDate.now(zone),
                                        onConfirm = { picked ->
                                            val newTs = deliveryTimestampForDate(picked, trip.pickupTimestamp)
                                            onEditDeliveryDate(trip, newTs)
                                            selectedTripData = trip.copy(deliveryTimestamp = newTs)
                                            showEditDeliveryPicker = false
                                        },
                                        onDismiss = { showEditDeliveryPicker = false }
                                    )
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
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
                                "Route Stops & Out-of-Route Miles",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            routeStops.forEachIndexed { index, stop ->
                                val actual = stop.actualLegMiles
                                val stopOor = actual?.let {
                                    (it - stop.dispatchedLegMiles).coerceAtLeast(0.0)
                                }
                                Text(
                                    "Stop Off ${index + 1}: ${stop.name} — dispatched ${String.format(Locale.US, "%.1f", stop.dispatchedLegMiles)} mi; " +
                                            (actual?.let {
                                                "tracked ${String.format(Locale.US, "%.1f", it)} mi; OOR ${String.format(Locale.US, "%.1f", stopOor ?: 0.0)} mi"
                                            } ?: "actual leg mileage not recorded"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if ((stopOor ?: 0.0) > 0.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            val finalLegOor = (finalLegActual - finalLegPlan).coerceAtLeast(0.0)
                            Text(
                                "Final consignee: dispatched ${String.format(Locale.US, "%.1f", finalLegPlan)} mi; " +
                                        "tracked ${String.format(Locale.US, "%.1f", finalLegActual)} mi; " +
                                        "OOR ${String.format(Locale.US, "%.1f", finalLegOor)} mi",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (finalLegOor > 0.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )

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
                            if (extraStopsPay > 0.0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Extra Stop Pay (${trip.extraStopCount} × $${String.format(Locale.US, "%.2f", extraStopRate)}):")
                                    Text("+$${String.format(Locale.US, "%.2f", extraStopsPay)}")
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
                val currentPayrollWeekStart = getPayrollWeekStartDate(System.currentTimeMillis())

                val weeklyTrips = pastLoads.filter { load ->
                    load.payrollWeekStartDate() == currentPayrollWeekStart
                }.sortedByNewestDelivery()

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
                            val extraStopsPay = trip.extraStopPayCents(extraStopRate).toDollars()
                            val totalTripPay =
                                baseDriverCut + tarpAddon + bounceBonus + extraStopsPay

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
                                    if (trip.extraStopCount > 0) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Extra Stop Pay (${trip.extraStopCount}):", style = MaterialTheme.typography.bodySmall)
                                            Text("+$${String.format(Locale.US, "%.2f", extraStopsPay)}", style = MaterialTheme.typography.bodySmall)
                                        }
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
            pastLoads.asSequence()
                .mapNotNull { load -> load.payrollWeekStartDate()?.let { it to load } }
                .groupBy({ it.first }, { it.second })
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
                            grouped.forEach { (weekStart, loads) ->
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
                                        base + tarp + bonus +
                                                load.extraStopPayCents(extraStopRate).toDollars()
                                    } + weekTrainerAddon
                                    Card(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            selectedWeekStart = weekStart
                                        },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(all = 16.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("Pay Week:", style = MaterialTheme.typography.labelSmall)
                                                Text(
                                                    "${weekStart.format(DateTimeFormatter.ofPattern("MM/dd"))} - ${weekStart.plusDays(6).format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))}",
                                                    style = MaterialTheme.typography.titleMedium
                                                )
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

    if (selectedWeekStart != null) {
        val weekStart = selectedWeekStart!!
        val weeklyTrips = pastLoads.filter { load ->
            load.payrollWeekStartDate() == weekStart
        }.sortedByNewestDelivery()

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
        val totalExtraStopPay = weeklyTrips.sumOf {
            it.extraStopPayCents(extraStopRate).toDollars()
        }
        val totalTrainerPay = weeklyTrips.maxOfOrNull { it.trainerPayRate }?.takeIf { it > 0.0 }
            ?: if (weeklyTrips.any { it.isTrainingWeek }) flatTrainerPayRate else 0.0
        val totalNetPay = totalDriverBase + totalTarpPay + totalBouncePay +
                totalExtraStopPay + totalTrainerPay

        AlertDialog(
            onDismissRequest = { selectedWeekStart = null },
            title = {
                Text(
                    "Statement: ${weekStart.format(DateTimeFormatter.ofPattern("MM/dd"))} - " +
                            weekStart.plusDays(6).format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))
                )
            },
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
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Extra Stop Pay:", style = MaterialTheme.typography.bodySmall)
                                        Text("$${String.format(Locale.US, "%.2f", totalExtraStopPay)}", style = MaterialTheme.typography.bodySmall)
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
                            val extraStopsPay = trip.extraStopPayCents(extraStopRate).toDollars()
                            val tripNet = baseCut + tarp + bounce + extraStopsPay

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
                                    Text(formatLoadDates(trip), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedWeekStart = null }) { Text("Back to List") }
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
                    mileageEditError = null
                    showMileageEditDialog = true
                }) { Text("Yes, Adjust Miles") }
            },
            dismissButton = {
                TextButton(onClick = { showUndoPromptDialog = false }) { Text("No, Keep Mileage As-Is") }
            }
        )
    }

    if (showMileageEditDialog) {
        val isBounceMileage = activeTrip?.tripState == "ACTIVE_SHIPPER" ||
                activeTrip?.tripState == "ACTIVE_BOUNCE"
        var correctedMilesStr by remember { mutableStateOf(mileageEditInput) }
        AlertDialog(
            onDismissRequest = {
                showMileageEditDialog = false
                mileageEditError = null
            },
            title = {
                Text(
                    stringResource(
                        if (isBounceMileage) R.string.mileage_edit_bounce_title
                        else R.string.mileage_edit_loaded_title
                    )
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(
                            if (isBounceMileage) R.string.mileage_edit_bounce_description
                            else R.string.mileage_edit_loaded_description
                        )
                    )
                    OutlinedTextField(
                        value = correctedMilesStr,
                        onValueChange = {
                            correctedMilesStr = it
                            mileageEditError = null
                        },
                        label = { Text(stringResource(R.string.mileage_edit_field_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = mileageEditError != null,
                        supportingText = mileageEditError?.let { error ->
                            { Text(stringResource(error)) }
                        }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val newMiles = correctedMilesStr.trim().toDoubleOrNull()
                    if (newMiles == null || !newMiles.isFinite() || newMiles < 0.0) {
                        mileageEditError = R.string.mileage_edit_invalid_error
                    } else if (activeTrip == null) {
                        mileageEditError = R.string.mileage_edit_no_active_load_error
                    } else {
                        if (isBounceMileage) {
                            TrackingService.totalBounceMilesTracked.value = newMiles
                            onUpdateTripClick(activeTrip.copy(bounceMilesEnd = newMiles))
                        } else {
                            TrackingService.totalLoadedMilesTracked.value = newMiles
                            onUpdateTripClick(activeTrip.copy(loadedMilesEnd = newMiles))
                        }
                        mileageEditError = null
                        showMileageEditDialog = false
                    }
                }) { Text(stringResource(R.string.mileage_edit_save)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showMileageEditDialog = false
                    mileageEditError = null
                }) {
                    Text(stringResource(R.string.mileage_edit_cancel))
                }
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

private fun extractPhoneNumber(text: String): Pair<String, String>? {
    if (text.isBlank()) return null
    val keywordRegex = Regex("""(?:call|contact|poc|phone|tel)\b.{0,20}?\(?\b(\d{3})\)?[-.\s]?(\d{3})[-.\s]?(\d{4})\b""", RegexOption.IGNORE_CASE)
    val keywordMatch = keywordRegex.find(text)

    if (keywordMatch != null && keywordMatch.groupValues.size >= 4) {
        val area = keywordMatch.groupValues[1]
        val mid = keywordMatch.groupValues[2]
        val end = keywordMatch.groupValues[3]
        return Pair("$area$mid$end", "($area) $mid-$end")
    }

    val standardRegex = Regex("""\(?\b(\d{3})\)?[-.\s]+(\d{3})[-.\s]+(\d{4})\b""")
    val standardMatch = standardRegex.find(text)

    if (standardMatch != null && standardMatch.groupValues.size >= 4) {
        val area = standardMatch.groupValues[1]
        val mid = standardMatch.groupValues[2]
        val end = standardMatch.groupValues[3]
        return Pair("$area$mid$end", "($area) $mid-$end")
    }

    return null
} 
