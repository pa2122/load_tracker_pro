package com.loadtracker.pro

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
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
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.StringBuilder
import java.time.Instant
import java.time.LocalDate
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
    val breadcrumbs by db.loadDao().getAllBreadcrumbs().collectAsState(initial = emptyList())

    var showAddFuelDialog by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<FuelEntry?>(null) }
    var selectedQuarterFilter by remember { mutableStateOf("ALL") }

    val currentYear = remember { LocalDate.now().year }

    val stateMileageMap = remember(breadcrumbs) { IftaStateDetector.calculateStateMileage(breadcrumbs) }

    val filteredFuelEntries = remember(fuelEntries, selectedQuarterFilter, currentYear) {
        if (selectedQuarterFilter == "ALL") {
            fuelEntries
        } else {
            fuelEntries.filter { entry ->
                val date = Instant.ofEpochMilli(entry.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                val month = date.monthValue
                val isCurrentYear = date.year == currentYear
                val quarterMatches = when (selectedQuarterFilter) {
                    "Q1" -> month in 1..3
                    "Q2" -> month in 4..6
                    "Q3" -> month in 7..9
                    "Q4" -> month in 10..12
                    else -> true
                }
                isCurrentYear && quarterMatches
            }
        }
    }

    // Analytics Calculations
    val totalGallons = filteredFuelEntries.sumOf { it.gallons }
    val totalCost = filteredFuelEntries.sumOf { it.totalCost }
    val avgPpg = if (totalGallons > 0) totalCost / totalGallons else 0.0

    val sortedEntries = remember(filteredFuelEntries) { filteredFuelEntries.sortedBy { it.odometer } }
    val calculatedMpg = remember(sortedEntries, totalGallons) {
        if (sortedEntries.size >= 2 && totalGallons > 0) {
            val totalMilesDriven = sortedEntries.last().odometer - sortedEntries.first().odometer
            if (totalMilesDriven > 0) totalMilesDriven / totalGallons else 0.0
        } else {
            0.0
        }
    }

    val stateIftaMap = remember(filteredFuelEntries) {
        filteredFuelEntries.groupBy { it.state.uppercase(Locale.US) }
    }

    fun exportIftaCsv() {
        try {
            val csvText = IftaTaxReportExporter.generateIftaCsv(selectedQuarterFilter, filteredFuelEntries, breadcrumbs)

            val file = File(ctx.filesDir, "ifta_tax_report_$selectedQuarterFilter.csv")
            file.writeText(csvText)

            val contentUri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "IFTA Fuel & Mileage Tax Report - $selectedQuarterFilter")
                putExtra(Intent.EXTRA_TEXT, "Attached is your official Load Tracker Pro IFTA Fuel & Mileage Tax Report ($selectedQuarterFilter).")
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            ctx.startActivity(Intent.createChooser(shareIntent, "Export IFTA Tax Report"))
        } catch (e: Exception) {
            Toast.makeText(ctx, "Error exporting IFTA report: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Quarterly Filter Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(selected = selectedQuarterFilter == "ALL", onClick = { selectedQuarterFilter = "ALL" }, label = { Text("All Time") })
                FilterChip(selected = selectedQuarterFilter == "Q1", onClick = { selectedQuarterFilter = "Q1" }, label = { Text("Q1") })
                FilterChip(selected = selectedQuarterFilter == "Q2", onClick = { selectedQuarterFilter = "Q2" }, label = { Text("Q2") })
                FilterChip(selected = selectedQuarterFilter == "Q3", onClick = { selectedQuarterFilter = "Q3" }, label = { Text("Q3") })
                FilterChip(selected = selectedQuarterFilter == "Q4", onClick = { selectedQuarterFilter = "Q4" }, label = { Text("Q4") })
            }

            // 1. MPG & Fuel Expenditure Analytics Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("⚡ Diesel Fuel & MPG Analytics ($selectedQuarterFilter)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)

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

            // 2. IFTA State Fuel Tax & Mileage Summary Card
            if (stateIftaMap.isNotEmpty() || stateMileageMap.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🗺️ IFTA State Tax & Mileage Audit ($selectedQuarterFilter)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        
                        val allStates = (stateIftaMap.keys + stateMileageMap.keys).toSet().sorted()
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            allStates.forEach { stateCode ->
                                val stateEntries = stateIftaMap[stateCode] ?: emptyList()
                                val stateGal = stateEntries.sumOf { it.gallons }
                                val stateMiles = stateMileageMap[stateCode] ?: 0.0

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(stateCode, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                    Text(
                                        "Fuel: ${String.format(Locale.US, "%.1f", stateGal)} gal | Miles: ${String.format(Locale.US, "%.1f", stateMiles)} mi",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Action Buttons Row (Log Fill-Up + Export CSV)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showAddFuelDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("⛽ Log Fill-Up")
                }

                Button(
                    onClick = { exportIftaCsv() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    enabled = filteredFuelEntries.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("📄 Export IFTA CSV")
                }
            }

            HorizontalDivider()

            // 4. Fuel Fill-Up History Ledger
            if (filteredFuelEntries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No fuel stops found for $selectedQuarterFilter.", color = MaterialTheme.colorScheme.secondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredFuelEntries) { entry ->
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
        var inputDefGallons by remember { mutableStateOf("") }
        var inputDefCost by remember { mutableStateOf("") }

        var stationDropdownExpanded by remember { mutableStateOf(false) }
        var stateDropdownExpanded by remember { mutableStateOf(false) }
        val stationOptions = listOf("Love's", "Pilot Flying J", "TA / Petro", "Speedway", "Kwik Trip", "Other")
        val usStateList = listOf("AL", "AZ", "AR", "CA", "CO", "CT", "DE", "FL", "GA", "ID", "IL", "IN", "IA", "KS", "KY", "LA", "ME", "MD", "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ", "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI", "SC", "SD", "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY")

        val receiptOcrLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                try {
                    val image = InputImage.fromFilePath(ctx, uri)
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    recognizer.process(image)
                        .addOnSuccessListener { visionText ->
                            val parsed = FuelReceiptOcrParser.parseReceiptText(visionText.text)
                            if (parsed.gallons != null) inputGallons = parsed.gallons.toString()
                            if (parsed.pricePerGallon != null) inputPpg = parsed.pricePerGallon.toString()
                            if (parsed.defGallons != null) inputDefGallons = parsed.defGallons.toString()
                            if (parsed.defCost != null) inputDefCost = String.format(Locale.US, "%.2f", parsed.defCost)
                            if (parsed.odometer != null) inputOdometer = parsed.odometer.toInt().toString()
                            if (parsed.state != null) inputState = parsed.state
                            if (parsed.stationName != null) inputStation = parsed.stationName
                            Toast.makeText(ctx, "Receipt screenshot parsed successfully!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(ctx, "Failed to scan receipt image.", Toast.LENGTH_SHORT).show()
                        }
                } catch (e: Exception) {
                    Toast.makeText(ctx, "Error opening receipt: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val galVal = inputGallons.toDoubleOrNull() ?: 0.0
        val ppgVal = inputPpg.toDoubleOrNull() ?: 0.0
        val computedTotalCost = galVal * ppgVal

        AlertDialog(
            onDismissRequest = { showAddFuelDialog = false },
            title = { Text("⛽ Log Diesel Fill-Up") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { receiptOcrLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📷 Scan / Attach Receipt Screenshot")
                    }

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
                        ExposedDropdownMenuBox(
                            expanded = stateDropdownExpanded,
                            onExpandedChange = { stateDropdownExpanded = !stateDropdownExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = inputState,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("State") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stateDropdownExpanded) },
                                modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
                            )
                            ExposedDropdownMenu(
                                expanded = stateDropdownExpanded,
                                onDismissRequest = { stateDropdownExpanded = false }
                            ) {
                                usStateList.forEach { st ->
                                    DropdownMenuItem(
                                        text = { Text(st) },
                                        onClick = {
                                            inputState = st
                                            stateDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = inputDefGallons,
                            onValueChange = { inputDefGallons = it },
                            label = { Text("DEF Gallons (Optional)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = inputDefCost,
                            onValueChange = { inputDefCost = it },
                            label = { Text("DEF Cost ($)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val gal = inputGallons.toDoubleOrNull()
                        val ppg = inputPpg.toDoubleOrNull()
                        val odo = inputOdometer.toDoubleOrNull()
                        val defGalVal = inputDefGallons.toDoubleOrNull() ?: 0.0
                        val defCostVal = inputDefCost.toDoubleOrNull() ?: 0.0

                        if (gal != null && ppg != null && odo != null && inputState.isNotBlank()) {
                            val totalCostCalc = gal * ppg
                            val newEntry = FuelEntry(
                                timestamp = System.currentTimeMillis(),
                                gallons = gal,
                                totalCost = totalCostCalc,
                                pricePerGallon = ppg,
                                state = inputState.trim().uppercase(Locale.US),
                                stationName = inputStation,
                                odometer = odo,
                                defGallons = defGalVal,
                                defCost = defCostVal
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
