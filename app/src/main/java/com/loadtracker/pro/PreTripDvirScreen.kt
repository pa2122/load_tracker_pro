package com.loadtracker.pro

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreTripDvirScreen(
    dvirEntries: List<DvirEntry>,
    onSaveDvir: (DvirEntry) -> Unit,
    onDeleteDvir: (DvirEntry) -> Unit,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    var showForm by remember { mutableStateOf(false) }

    // Form State
    var truckNum by remember { mutableStateOf("") }
    var trailerNum by remember { mutableStateOf("") }
    var odometerStr by remember { mutableStateOf("") }
    
    var passedTractor by remember { mutableStateOf(true) }
    var passedCoupling by remember { mutableStateOf(true) }
    var passedBrakes by remember { mutableStateOf(true) }
    var passedFlatbedGear by remember { mutableStateOf(true) }

    var strapsCount by remember { mutableIntStateOf(10) }
    var chainsBindersCount by remember { mutableIntStateOf(6) }
    var tarpsCond by remember { mutableStateOf("Good") } // "Good", "Needs Repair", "Missing"
    var coilRacksCount by remember { mutableIntStateOf(4) }

    var defectsText by remember { mutableStateOf("") }
    var isSafeToOperate by remember { mutableStateOf(true) }
    var driverSig by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (showForm) "New Pre-Trip DVIR" else "Pre-Trip Inspection (DVIR)") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (showForm) showForm = false else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (!showForm) {
                FloatingActionButton(onClick = { showForm = true }) {
                    Icon(Icons.Default.Add, contentDescription = "New DVIR")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (showForm) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text("Vehicle Details", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = truckNum,
                                onValueChange = { truckNum = it },
                                label = { Text("Tractor #") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = trailerNum,
                                onValueChange = { trailerNum = it },
                                label = { Text("Trailer #") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        OutlinedTextField(
                            value = odometerStr,
                            onValueChange = { odometerStr = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Current Odometer") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                            singleLine = true
                        )
                    }

                    item {
                        HorizontalDivider()
                        Text("Safety Checkpoints", style = MaterialTheme.typography.titleMedium)
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = passedTractor, onCheckedChange = { passedTractor = it })
                            Text("Tractor & Cab (Air brakes, Steering, Lights, Oil)")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = passedCoupling, onCheckedChange = { passedCoupling = it })
                            Text("Coupling & 5th Wheel (Kingpin, Jaws, Air lines)")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = passedBrakes, onCheckedChange = { passedBrakes = it })
                            Text("Tires, Rims & Glad Hands (Tread, Lug nuts)")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = passedFlatbedGear, onCheckedChange = { passedFlatbedGear = it })
                            Text("Flatbed Open-Deck Gear (Straps, Chains, Tarps)")
                        }
                    }

                    item {
                        HorizontalDivider()
                        Text("Flatbed Securement Gear Audit", style = MaterialTheme.typography.titleMedium)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Winch Straps (WLL Tagged):")
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (strapsCount > 0) strapsCount-- }) { Text("-") }
                                Text("$strapsCount", fontWeight = FontWeight.Bold)
                                IconButton(onClick = { strapsCount++ }) { Text("+") }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Grade 70 Chains & Binders:")
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (chainsBindersCount > 0) chainsBindersCount-- }) { Text("-") }
                                Text("$chainsBindersCount", fontWeight = FontWeight.Bold)
                                IconButton(onClick = { chainsBindersCount++ }) { Text("+") }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Coil Racks & Timbers:")
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (coilRacksCount > 0) coilRacksCount-- }) { Text("-") }
                                Text("$coilRacksCount", fontWeight = FontWeight.Bold)
                                IconButton(onClick = { coilRacksCount++ }) { Text("+") }
                            }
                        }

                        Text("Tarp Condition:", style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Good", "Needs Repair", "Missing").forEach { cond ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = (tarpsCond == cond), onClick = { tarpsCond = cond })
                                    Text(cond)
                                }
                            }
                        }
                    }

                    item {
                        HorizontalDivider()
                        Text("Defects & Sign-Off", style = MaterialTheme.typography.titleMedium)

                        OutlinedTextField(
                            value = defectsText,
                            onValueChange = { 
                                defectsText = it
                                if (it.isNotBlank() && isSafeToOperate) {
                                    // Default to unsafe if defects noted
                                    isSafeToOperate = false
                                }
                            },
                            label = { Text("Defects Found (Leave blank if none)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isSafeToOperate, onCheckedChange = { isSafeToOperate = it })
                            Text("Vehicle Condition Safe to Operate", fontWeight = FontWeight.Bold)
                        }

                        OutlinedTextField(
                            value = driverSig,
                            onValueChange = { driverSig = it },
                            label = { Text("Driver Signature / Full Name (Required)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                val odo = odometerStr.toDoubleOrNull() ?: 0.0
                                if (truckNum.isBlank() || driverSig.isBlank()) {
                                    Toast.makeText(ctx, "Please enter Tractor # and Driver Signature.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                val newEntry = DvirEntry(
                                    timestamp = System.currentTimeMillis(),
                                    truckNumber = truckNum.trim(),
                                    trailerNumber = trailerNum.trim(),
                                    odometer = odo,
                                    passedTractorCheck = passedTractor,
                                    passedCouplingCheck = passedCoupling,
                                    passedBrakesTiresCheck = passedBrakes,
                                    passedFlatbedGearCheck = passedFlatbedGear,
                                    strapsCount = strapsCount,
                                    chainsBindersCount = chainsBindersCount,
                                    tarpsCondition = tarpsCond,
                                    coilRacksCount = coilRacksCount,
                                    defectsFound = defectsText.ifBlank { null },
                                    isSafeToOperate = isSafeToOperate,
                                    driverSignature = driverSig.trim()
                                )

                                onSaveDvir(newEntry)
                                showForm = false
                                Toast.makeText(ctx, "Pre-Trip DVIR Saved Successfully!", Toast.LENGTH_SHORT).show()
                            },
                            enabled = truckNum.isNotBlank() && driverSig.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Sign & Save Pre-Trip DVIR")
                        }
                    }
                }
            } else {
                if (dvirEntries.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No Pre-Trip Inspection logs found.", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Tap '+' to complete a digital FMCSA Pre-Trip DVIR.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(dvirEntries) { entry ->
                            val dateStr = remember(entry.timestamp) {
                                Instant.ofEpochMilli(entry.timestamp)
                                    .atZone(ZoneId.systemDefault())
                                    .format(DateTimeFormatter.ofPattern("MM/dd/yyyy h:mm a", Locale.US))
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (entry.isSafeToOperate) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                if (entry.isSafeToOperate) Icons.Default.CheckCircle else Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = if (entry.isSafeToOperate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                if (entry.isSafeToOperate) "SAFE TO OPERATE" else "DEFECTS NOTED - UNSAFE",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = if (entry.isSafeToOperate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                            )
                                        }
                                        Text(dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                    }

                                    HorizontalDivider()

                                    Text("Tractor: #${entry.truckNumber} | Trailer: #${entry.trailerNumber} | Odo: ${String.format(Locale.US, "%.0f", entry.odometer)} mi", style = MaterialTheme.typography.bodyMedium)
                                    Text("Flatbed Gear: ${entry.strapsCount} Straps, ${entry.chainsBindersCount} Chains/Binders, ${entry.coilRacksCount} Coil Racks, Tarps: ${entry.tarpsCondition}", style = MaterialTheme.typography.bodySmall)

                                    if (!entry.defectsFound.isNullOrBlank()) {
                                        Text("Defects: ${entry.defectsFound}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                    }

                                    Text("Signed by Driver: ${entry.driverSignature}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
