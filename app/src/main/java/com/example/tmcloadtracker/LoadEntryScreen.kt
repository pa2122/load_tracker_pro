package com.example.tmcloadtracker

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadEntryScreen(
    initialPercentage: String,
    onSaveClick: (CurrentLoad) -> Unit,
    onCancelClick: () -> Unit
) {
    val ctx = LocalContext.current
    val scroll = rememberScrollState()

    var proNum by remember { mutableStateOf("") }
    var dBounce by remember { mutableStateOf("") }
    var dLoaded by remember { mutableStateOf("") }
    var ratePct by remember { mutableStateOf(initialPercentage) }
    var loadPay by remember { mutableStateOf("") }

    var expanded by remember { mutableStateOf(false) }
    val tarpOpts = listOf("N (None)", "S (Steel)", "L (Lumber)")
    var selectedTarp by remember { mutableStateOf("N (None)") }

    var isPreTarped by remember { mutableStateOf(false) }
    var isGoingHome by remember { mutableStateOf(false) }

    var tripStatus by remember { mutableStateOf("NOT_STARTED") }

    val liveBounce by TrackingService.totalBounceMilesTracked.collectAsState()
    val liveLoaded by TrackingService.totalLoadedMilesTracked.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Initialize New Freight Load", style = MaterialTheme.typography.headlineMedium)

        // PRO # Field: Strips everything except digits
        OutlinedTextField(
            value = proNum,
            onValueChange = { input -> proNum = input.filter { it.isDigit() } },
            label = { Text("PRO # (Required - Numbers Only)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider()

        Text("Dispatched Miles", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Dispatched Bounce: Numbers and single decimal filter
            OutlinedTextField(
                value = dBounce,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() || it == '.' }
                    if (filtered.count { it == '.' } <= 1) {
                        dBounce =
                            if (filtered.startsWith("0") && filtered.length > 1 && !filtered.startsWith(
                                    "0."
                                )
                            ) {
                                filtered.dropWhile { it == '0' }
                            } else filtered
                    }
                },
                label = { Text("Disp. Bounce *") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.weight(1f)
            )
            // Dispatched Loaded: Numbers and single decimal filter
            OutlinedTextField(
                value = dLoaded,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() || it == '.' }
                    if (filtered.count { it == '.' } <= 1) {
                        dLoaded =
                            if (filtered.startsWith("0") && filtered.length > 1 && !filtered.startsWith(
                                    "0."
                                )
                            ) {
                                filtered.dropWhile { it == '0' }
                            } else filtered
                    }
                },
                label = { Text("Disp. Loaded *") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.weight(1f)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isGoingHome, onCheckedChange = { isGoingHome = it })
            Text("Going Home Load")
        }

        HorizontalDivider()

        Text("Revenue & Accessories Details", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = ratePct,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() || it == '.' }
                    if (filtered.isEmpty() || filtered == ".") ratePct = filtered
                    else if (filtered.count { it == '.' } <= 1) {
                        val num = filtered.toDoubleOrNull()
                        if (num != null && num >= 0.0 && num <= 100.0) ratePct = filtered
                    }
                },
                label = { Text("Pay Split (%)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.weight(1f)
            )
            // Gross Pay: Supports dollars and cents formatting, drops leading zeros
            OutlinedTextField(
                value = loadPay,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() || it == '.' }
                    if (filtered.count { it == '.' } <= 1) {
                        loadPay =
                            if (filtered.startsWith("0") && filtered.length > 1 && !filtered.startsWith(
                                    "0."
                                )
                            ) {
                                filtered.dropWhile { it == '0' }
                            } else filtered
                    }
                },
                label = { Text("Gross Pay ($) *") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.weight(1f)
            )
        }

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = selectedTarp,
                onValueChange = {},
                readOnly = true,
                label = { Text("Tarp Type") },
                trailingIcon = {
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            imageVector = if (expanded) {
                                androidx.compose.material.icons.Icons.Default.KeyboardArrowUp
                            } else {
                                androidx.compose.material.icons.Icons.Default.ArrowDropDown
                            },
                            contentDescription = "Toggle Tarp Menu"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                tarpOpts.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = { selectedTarp = option; expanded = false }
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isPreTarped, onCheckedChange = { isPreTarped = it })
            Text("Load Pre-Tarped at Shipper (Halves tarp bonus)")
        }

        HorizontalDivider()
        Text("Trip Operations Panel", style = MaterialTheme.typography.titleMedium)

        when (tripStatus) {
            "NOT_STARTED" -> {
                // 📍 THE SECURITY GATE: Verifies required fields are not blank
                val isFormValid = proNum.trim().isNotEmpty() &&
                        dBounce.trim().isNotEmpty() &&
                        dLoaded.trim().isNotEmpty() &&
                        loadPay.trim().isNotEmpty()

                Button(
                    onClick = {
                        TrackingService.totalBounceMilesTracked.value = 0.0
                        TrackingService.totalLoadedMilesTracked.value = 0.0
                        TrackingService.activeSegment = "Bounce"
                        ctx.startService(Intent(ctx, TrackingService::class.java))

                        val tChar = when {
                            selectedTarp.contains("Lumber") || selectedTarp.contains("L") -> "L"
                            selectedTarp.contains("Steel") || selectedTarp.contains("S") -> "S"
                            else -> "N"
                        }

                        val timeStr = System.currentTimeMillis().toString()
                        val draftData = CurrentLoad(
                            proNumber = proNum.trim(),
                            dispatchedBounceMiles = dBounce.toDoubleOrNull() ?: 0.0,
                            dispatchedLoadedMiles = dLoaded.toDoubleOrNull() ?: 0.0,
                            bounceMilesStart = 0.0, bounceMilesEnd = 0.0,
                            loadedMilesStart = 0.0, loadedMilesEnd = 0.0,
                            percentageRate = ratePct.toDoubleOrNull() ?: 80.0,
                            loadPay = loadPay.toDoubleOrNull() ?: 0.0,
                            tarpType = tChar,
                            isPreTarped = isPreTarped,
                            isGoingHome = isGoingHome,
                            pickupTimestamp = System.currentTimeMillis(),
                            tripState = "ACTIVE_BOUNCE"
                        )
                        onSaveClick(draftData)
                    },
                    // 📍 LOCKS DOWN THE BUTTON IF INPUT CONDITIONS ARE NOT MET
                    enabled = isFormValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Start Load (Dispatch En Route)")
                }
            }

            else -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val lbl = when (tripStatus) {
                            "EN_ROUTE_SHIPPER" -> "STATUS: EN ROUTE TO SHIPPER"
                            "AT_SHIPPER" -> "STATUS: LOADING AT SHIPPER"
                            "EN_ROUTE_CONSIGNEE" -> "STATUS: EN ROUTE TO CONSIGNEE"
                            else -> ""
                        }
                        Text(
                            text = lbl,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Live Bounce")
                                Text(
                                    "${String.format("%.1f", liveBounce)} mi",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Live Loaded")
                                Text(
                                    "${String.format("%.1f", liveLoaded)} mi",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        when (tripStatus) {
                            "EN_ROUTE_SHIPPER" -> {
                                Button(
                                    onClick = {
                                        TrackingService.activeSegment = "Paused"
                                        tripStatus = "AT_SHIPPER"
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Arrive at Shipper") }
                            }

                            "AT_SHIPPER" -> {
                                Button(
                                    onClick = {
                                        TrackingService.activeSegment = "Loaded"
                                        tripStatus = "EN_ROUTE_CONSIGNEE"
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Depart Shipper (Loaded)") }
                            }

                            "EN_ROUTE_CONSIGNEE" -> {
                                Button(
                                    onClick = {
                                        ctx.stopService(Intent(ctx, TrackingService::class.java))
                                        val tChar = when {
                                            selectedTarp.contains("Lumber") -> "L"
                                            selectedTarp.contains("Steel") -> "S"
                                            else -> "N"
                                        }
                                        val timeStr = System.currentTimeMillis().toString()
                                        val finalData = CurrentLoad(
                                            proNumber = proNum.trim(),
                                            dispatchedBounceMiles = dBounce.toDoubleOrNull() ?: 0.0,
                                            dispatchedLoadedMiles = dLoaded.toDoubleOrNull() ?: 0.0,
                                            bounceMilesStart = 0.0,
                                            bounceMilesEnd = liveBounce,
                                            loadedMilesStart = 0.0,
                                            loadedMilesEnd = liveLoaded,
                                            percentageRate = ratePct.toDoubleOrNull() ?: 80.0,
                                            loadPay = loadPay.toDoubleOrNull() ?: 0.0,
                                            tarpType = tChar,
                                            isPreTarped = isPreTarped,
                                            isGoingHome = isGoingHome,
                                            pickupTimestamp = System.currentTimeMillis(),
                                            tripState = "COMPLETED"
                                        )
                                        onSaveClick(finalData)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Arrive at Consignee (Complete)") }
                            }
                        }
                    }
                }
            }
        }

        TextButton(
            onClick = {
                if (tripStatus != "NOT_STARTED") {
                    ctx.stopService(Intent(ctx, TrackingService::class.java))
                }
                onCancelClick()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel Load Entry")
        }
    }
}
