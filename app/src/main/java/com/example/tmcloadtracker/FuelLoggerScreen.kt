package com.example.tmcloadtracker

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelLoggerScreen(
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(ctx) }

    val fuelEntries by db.loadDao().getAllFuelEntries().collectAsState(initial = emptyList())

    var showAddFuelDialog by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<FuelEntry?>(null) }

    // Analytics Calculations
    val totalGallons = fuelEntries.sumOf { it.gallons }
    val totalCost = fuelEntries.sumOf { it.totalCost }
    val avgPpg = if (totalGallons > 0) totalCost / totalGallons else 0.0

    val sortedEntries = remember(fuelEntries) { fuelEntries.sortedBy { it.odometer } }
    val calculatedMpg = remember(sortedEntries, totalGallons) {
        if (sortedEntries.size >= 2 && totalGallons > 0) {
            val totalMilesDriven = sortedEntries.last().odometer - sortedEntries.first().odometer
            if (totalMilesDriven > 0) totalMilesDriven / totalGallons else 0.0
        } else {
            0.0
        }
    }

    val stateIftaMap = remember(fuelEntries) {
        fuelEntries.groupBy { it.state.uppercase(Locale.US) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⛽ Fuel Logger & IFTA Tracker") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. MPG & Fuel Expenditure Analytics Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("⚡ Diesel Fuel & MPG Analytics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Avg Miles/Gallon (MPG)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(
                                if (calculatedMpg > 0) "${String.format(Locale.US, "%.2f", calculatedMpg)} MPG" else "Need 2+ Fill-ups",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Avg Price / Gallon", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("$${String.format(Locale.US, "%.3f", avgPpg)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Gallons: ${String.format(Locale.US, "%.1f", totalGallons)} gal", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Total Cost: $${String.format(Locale.US, "%.2f", totalCost)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            // 2. IFTA State Fuel Tax Summary Card
            if (stateIftaMap.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🗺️ IFTA Fuel Tax Summary by State", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            stateIftaMap.forEach { (stateCode, entries) ->
                                val stateGal = entries.sumOf { it.gallons }
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.padding(2.dp)
                                ) {
                                    Text(
                                        "$stateCode: ${String.format(Locale.US, "%.1f", stateGal)} gal",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Log Fuel Stop Button
            Button(
                onClick = { showAddFuelDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("⛽ Log Diesel Fill-Up")
            }

            HorizontalDivider()

            // 4. Fuel Fill-Up History Ledger
            if (fuelEntries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No fuel stops logged yet. Tap 'Log Diesel Fill-Up' above.", color = MaterialTheme.colorScheme.secondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(fuelEntries) { entry ->
                        val dateStr = Instant.ofEpochMilli(entry.timestamp)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                            .format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(4.dp)) {
                                            Text(entry.state.uppercase(Locale.US), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                        Text(entry.stationName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    }
                                    Text("$${String.format(Locale.US, "%.2f", entry.totalCost)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("• ${String.format(Locale.US, "%.1f", entry.gallons)} gal @ $${String.format(Locale.US, "%.3f", entry.pricePerGallon)}/gal", style = MaterialTheme.typography.bodySmall)
                                    Text("Odo: ${entry.odometer.toInt()} mi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                    IconButton(onClick = { entryToDelete = entry }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Fuel Stop", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddFuelDialog) {
        var inputGallons by remember { mutableStateOf("") }
        var inputPpg by remember { mutableStateOf("") }
        var inputState by remember { mutableStateOf("TX") }
        var inputStation by remember { mutableStateOf("Love's") }
        var inputOdometer by remember { mutableStateOf("") }

        var stationDropdownExpanded by remember { mutableStateOf(false) }
        val stationOptions = listOf("Love's", "Pilot Flying J", "TA / Petro", "Speedway", "Kwik Trip", "Other")

        val galVal = inputGallons.toDoubleOrNull() ?: 0.0
        val ppgVal = inputPpg.toDoubleOrNull() ?: 0.0
        val computedTotalCost = galVal * ppgVal

        AlertDialog(
            onDismissRequest = { showAddFuelDialog = false },
            title = { Text("⛽ Log Diesel Fill-Up") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = inputGallons,
                            onValueChange = { inputGallons = it },
                            label = { Text("Gallons") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = inputPpg,
                            onValueChange = { inputPpg = it },
                            label = { Text("Price / Gal ($)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text("Computed Total Cost: $${String.format(Locale.US, "%.2f", computedTotalCost)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = inputState,
                            onValueChange = { inputState = it.uppercase(Locale.US).take(2) },
                            label = { Text("State (e.g. TX)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        ExposedDropdownMenuBox(
                            expanded = stationDropdownExpanded,
                            onExpandedChange = { stationDropdownExpanded = !stationDropdownExpanded },
                            modifier = Modifier.weight(1.5f)
                        ) {
                            OutlinedTextField(
                                value = inputStation,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Station") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stationDropdownExpanded) },
                                modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
                            )
                            ExposedDropdownMenu(
                                expanded = stationDropdownExpanded,
                                onDismissRequest = { stationDropdownExpanded = false }
                            ) {
                                stationOptions.forEach { opt ->
                                    DropdownMenuItem(
                                        text = { Text(opt) },
                                        onClick = {
                                            inputStation = opt
                                            stationDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = inputOdometer,
                        onValueChange = { inputOdometer = it.filter { char -> char.isDigit() } },
                        label = { Text("Odometer Reading (mi)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val gal = inputGallons.toDoubleOrNull()
                        val ppg = inputPpg.toDoubleOrNull()
                        val odo = inputOdometer.toDoubleOrNull()

                        if (gal != null && ppg != null && odo != null && inputState.isNotBlank()) {
                            val totalCostCalc = gal * ppg
                            val newEntry = FuelEntry(
                                timestamp = System.currentTimeMillis(),
                                gallons = gal,
                                totalCost = totalCostCalc,
                                pricePerGallon = ppg,
                                state = inputState.trim().uppercase(Locale.US),
                                stationName = inputStation,
                                odometer = odo
                            )

                            scope.launch(Dispatchers.IO) {
                                db.loadDao().insertFuelEntry(newEntry)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(ctx, "Fuel stop logged successfully!", Toast.LENGTH_SHORT).show()
                                    showAddFuelDialog = false
                                }
                            }
                        } else {
                            Toast.makeText(ctx, "Please enter valid Gallons, Price/Gal, State, and Odometer.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text("Save Fuel Stop") }
            },
            dismissButton = {
                TextButton(onClick = { showAddFuelDialog = false }) { Text("Cancel") }
            }
        )
    }

    val deletingEntry = entryToDelete
    if (deletingEntry != null) {
        val dateStr = Instant.ofEpochMilli(deletingEntry.timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))

        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("🗑️ Delete Fuel Fill-Up?") },
            text = { Text("Delete $dateStr fuel stop at ${deletingEntry.stationName} (${deletingEntry.gallons} gal / $${deletingEntry.totalCost})?") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            db.loadDao().deleteFuelEntry(deletingEntry)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(ctx, "Deleted fuel stop!", Toast.LENGTH_SHORT).show()
                                entryToDelete = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) { Text("Cancel") }
            }
        )
    }
}
