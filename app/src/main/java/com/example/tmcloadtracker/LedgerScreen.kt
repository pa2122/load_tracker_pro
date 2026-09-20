package com.example.tmcloadtracker

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(
    viewModel: LoadViewModel,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("dev_prefs", Context.MODE_PRIVATE) }

    var payrollCycleMode by remember {
        mutableStateOf(prefs.getString("payroll_cycle_mode", "weekly_friday") ?: "weekly_friday")
    }
    var cycleDropdownExpanded by remember { mutableStateOf(false) }

    val allLoads by viewModel.allLoads.collectAsState(initial = emptyList())
    val completedLoads = remember(allLoads) {
        allLoads.filter { it.tripState == "COMPLETED" }
    }

    // P&L Metrics Calculation
    val totalGross = completedLoads.sumOf { it.loadPay }
    val totalLoadedMiles = completedLoads.sumOf { if (it.loadedMilesEnd > 0) it.loadedMilesEnd else it.dispatchedLoadedMiles }
    val totalBounceMiles = completedLoads.sumOf { if (it.bounceMilesEnd > 0) it.bounceMilesEnd else it.dispatchedBounceMiles }
    val totalMiles = totalLoadedMiles + totalBounceMiles

    val rpm = if (totalLoadedMiles > 0) totalGross / totalLoadedMiles else 0.0

    // Overall Driver Take-Home calculation
    val totalTakeHome = completedLoads.sumOf { load ->
        val cut = load.loadPay * (load.percentageRate / 100.0)
        val tarp = when (load.tarpType) {
            "S" -> if (load.isPreTarped) 15.0 else 30.0
            "L" -> if (load.isPreTarped) 25.0 else 50.0
            else -> 0.0
        }
        val dh = if (load.dispatchedBounceMiles >= 150.0) load.dispatchedBounceMiles * 0.20 else 0.0
        val trainer = if (load.isTrainingWeek) load.trainerPayRate else 0.0
        cut + tarp + dh + trainer
    }

    val netRpm = if (totalMiles > 0) totalTakeHome / totalMiles else 0.0

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Settlement Cycle Mode Switcher
            ExposedDropdownMenuBox(
                expanded = cycleDropdownExpanded,
                onExpandedChange = { cycleDropdownExpanded = !cycleDropdownExpanded }
            ) {
                val cycleLabel = if (payrollCycleMode == "immediate_paperwork") "⚡ Immediate / Paperwork Submitted Date" else "📅 Weekly (Friday Cutoff)"
                OutlinedTextField(
                    value = cycleLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Pay Settlement Cycle") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cycleDropdownExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
                )
                ExposedDropdownMenu(
                    expanded = cycleDropdownExpanded,
                    onDismissRequest = { cycleDropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("📅 Weekly (Friday Cutoff)") },
                        onClick = {
                            payrollCycleMode = "weekly_friday"
                            prefs.edit().putString("payroll_cycle_mode", "weekly_friday").apply()
                            cycleDropdownExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("⚡ Immediate / Paperwork Submitted Date") },
                        onClick = {
                            payrollCycleMode = "immediate_paperwork"
                            prefs.edit().putString("payroll_cycle_mode", "immediate_paperwork").apply()
                            cycleDropdownExpanded = false
                        }
                    )
                }
            }

            // 2. Owner-Op P&L Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📈 Owner-Op Profit & Loss Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Gross Revenue", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("$${String.format(Locale.US, "%.2f", totalGross)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Driver Take-Home", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("$${String.format(Locale.US, "%.2f", totalTakeHome)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Gross RPM: $${String.format(Locale.US, "%.2f", rpm)}/mi", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Net RPM: $${String.format(Locale.US, "%.2f", netRpm)}/mi", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Text("Total Miles Tracked: ${totalMiles.toInt()} mi (${totalLoadedMiles.toInt()} Loaded / ${totalBounceMiles.toInt()} Bounce)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            HorizontalDivider()

            // 3. Settlement Statements Grouped List
            if (completedLoads.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No completed settlement records found.", color = MaterialTheme.colorScheme.secondary)
                }
            } else {
                if (payrollCycleMode == "immediate_paperwork") {
                    // Group by exact Delivery / Submission Date
                    val groupedByDate = remember(completedLoads) {
                        completedLoads.groupBy { load ->
                            val delivTs = load.deliveryTimestamp ?: 0L
                            val ts = if (delivTs > 0L) delivTs else load.pickupTimestamp
                            Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(groupedByDate.entries.toList()) { (dateStr, loadsInGroup) ->
                            val dayGross = loadsInGroup.sumOf { it.loadPay }
                            val dayTakeHome = loadsInGroup.sumOf { load ->
                                val cut = load.loadPay * (load.percentageRate / 100.0)
                                val tarp = when (load.tarpType) {
                                    "S" -> if (load.isPreTarped) 15.0 else 30.0
                                    "L" -> if (load.isPreTarped) 25.0 else 50.0
                                    else -> 0.0
                                }
                                val dh = if (load.dispatchedBounceMiles >= 150.0) load.dispatchedBounceMiles * 0.20 else 0.0
                                val trainer = if (load.isTrainingWeek) load.trainerPayRate else 0.0
                                cut + tarp + dh + trainer
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("⚡ Settled: $dateStr", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text("$${String.format(Locale.US, "%.2f", dayTakeHome)} Take-Home", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Text("Gross Revenue: $${String.format(Locale.US, "%.2f", dayGross)} (${loadsInGroup.size} Load${if (loadsInGroup.size > 1) "s" else ""})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)

                                    loadsInGroup.forEach { load ->
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("PRO #${load.proNumber} (${load.shipperName?.lines()?.firstOrNull() ?: "Shipper"})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                            Text("$${String.format(Locale.US, "%.2f", load.loadPay)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Group by Week-Ending Friday Cutoff
                    val groupedByFriday = remember(completedLoads) {
                        completedLoads.groupBy { load ->
                            getPayPeriodDate(load.pickupTimestamp).format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(groupedByFriday.entries.toList()) { (fridayStr, loadsInGroup) ->
                            val weekGross = loadsInGroup.sumOf { it.loadPay }
                            val weekTakeHome = loadsInGroup.sumOf { load ->
                                val cut = load.loadPay * (load.percentageRate / 100.0)
                                val tarp = when (load.tarpType) {
                                    "S" -> if (load.isPreTarped) 15.0 else 30.0
                                    "L" -> if (load.isPreTarped) 25.0 else 50.0
                                    else -> 0.0
                                }
                                val dh = if (load.dispatchedBounceMiles >= 150.0) load.dispatchedBounceMiles * 0.20 else 0.0
                                val trainer = if (load.isTrainingWeek) load.trainerPayRate else 0.0
                                cut + tarp + dh + trainer
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("📅 Week Ending Friday: $fridayStr", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text("$${String.format(Locale.US, "%.2f", weekTakeHome)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Text("Gross Revenue: $${String.format(Locale.US, "%.2f", weekGross)} (${loadsInGroup.size} Load${if (loadsInGroup.size > 1) "s" else ""})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)

                                    loadsInGroup.forEach { load ->
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("PRO #${load.proNumber} (${load.shipperName?.lines()?.firstOrNull() ?: "Shipper"})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                            Text("$${String.format(Locale.US, "%.2f", load.loadPay)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
