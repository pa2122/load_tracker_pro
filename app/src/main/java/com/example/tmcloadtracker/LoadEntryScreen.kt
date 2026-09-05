package com.example.tmcloadtracker

import android.content.Intent
import android.location.Geocoder
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
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
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadEntryScreen(
    initialPercentage: String,
    editingLoad: CurrentLoad? = null,
    onSaveClick: (CurrentLoad) -> Unit,
    onCancelClick: () -> Unit
) {
    val ctx = LocalContext.current
    val scroll = rememberScrollState()

    var proNum by remember { mutableStateOf(editingLoad?.proNumber ?: "") }
    var dBounce by remember { mutableStateOf(editingLoad?.dispatchedBounceMiles?.toString() ?: "") }
    var dLoaded by remember { mutableStateOf(editingLoad?.dispatchedLoadedMiles?.toString() ?: "") }
    var ratePct by remember { mutableStateOf(editingLoad?.percentageRate?.toString() ?: initialPercentage) }
    var loadPay by remember { mutableStateOf(editingLoad?.loadPay?.toString() ?: "") }

    var expanded by remember { mutableStateOf(false) }
    val tarpOpts = listOf("None", "4' Drop", "8' Drop")
    var selectedTarp by remember {
        val type = editingLoad?.tarpType ?: "N"
        mutableStateOf(when(type) {
            "S" -> "4' Drop"
            "L" -> "8' Drop"
            else -> "None"
        })
    }

    var isPreTarped by remember { mutableStateOf(editingLoad?.isPreTarped ?: false) }
    var isGoingHome by remember { mutableStateOf(editingLoad?.isGoingHome ?: false) }
    var tripNotes by remember { mutableStateOf(editingLoad?.tripNotes ?: "") }
    var isManualEntry by remember { mutableStateOf(false) }

    var manualActBounce by remember { mutableStateOf("") }
    var manualActLoaded by remember { mutableStateOf("") }
    var matchDispatched by remember { mutableStateOf(true) }

    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    val dateLabel = remember(selectedDateMillis) {
        val instant = Instant.ofEpochMilli(selectedDateMillis)
        val zone = ZoneId.systemDefault()
        val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
        instant.atZone(zone).toLocalDate().format(formatter)
    }

    var sRawPaste by remember { mutableStateOf("") }
    var sName by remember { mutableStateOf(editingLoad?.shipperName ?: "") }
    var sLat by remember { mutableStateOf(editingLoad?.shipperLat) }
    var sLong by remember { mutableStateOf(editingLoad?.shipperLong) }

    var cRawPaste by remember { mutableStateOf("") }
    var cName by remember { mutableStateOf(editingLoad?.consigneeName ?: "") }
    var cLat by remember { mutableStateOf(editingLoad?.consigneeLat) }
    var cLong by remember { mutableStateOf(editingLoad?.consigneeLong) }

    fun resolveAddress(input: String, isShipper: Boolean) {
        val lines = input.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return

        val name = lines[0].trim()
        val addressPart = if (lines.size > 1) lines.drop(1).joinToString(", ").trim() else name

        if (isShipper) sName = name else cName = name

        try {
            val geocoder = Geocoder(ctx, Locale.US)
            @Suppress("DEPRECATION")
            val results = geocoder.getFromLocationName(addressPart, 1)
            if (!results.isNullOrEmpty()) {
                val loc = results[0]
                if (isShipper) {
                    sLat = loc.latitude
                    sLong = loc.longitude
                } else {
                    cLat = loc.latitude
                    cLong = loc.longitude
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    var showFridayReminder by remember { mutableStateOf(false) }
    var pendingLoadSave by remember { mutableStateOf<CurrentLoad?>(null) }

    fun processSave(load: CurrentLoad) {
        val dateToCheck = Instant.ofEpochMilli(load.pickupTimestamp)
            .atZone(ZoneId.systemDefault()).toLocalDate()

        if (!isManualEntry && dateToCheck.dayOfWeek == DayOfWeek.FRIDAY && !load.isGoingHome) {
            pendingLoadSave = load
            showFridayReminder = true
        } else {
            onSaveClick(load)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(if (editingLoad == null) "New Freight Load" else "Edit Freight Load", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = proNum,
            onValueChange = { input -> proNum = input.filter { it.isDigit() } },
            label = { Text("PRO # (Required)") },
            singleLine = true,
            enabled = editingLoad == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider()

        Text("Shipper Details (Required)", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = sRawPaste,
            onValueChange = { sRawPaste = it },
            label = { Text("Paste Shipper Name & Address") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = if (sName.isNotEmpty()) "Name: $sName" else "Name: Not Parsed", style = MaterialTheme.typography.bodySmall)
                Text(text = if (sLat != null) "Location: Fixed 📍" else "Location: Pending", style = MaterialTheme.typography.bodySmall, color = if (sLat != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
            Button(onClick = { resolveAddress(sRawPaste, true) }, enabled = sRawPaste.isNotBlank()) { Text("Verify") }
        }

        HorizontalDivider()

        Text("Consignee Details (Required)", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = cRawPaste,
            onValueChange = { cRawPaste = it },
            label = { Text("Paste Consignee Name & Address") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = if (cName.isNotEmpty()) "Name: $cName" else "Name: Not Parsed", style = MaterialTheme.typography.bodySmall)
                Text(text = if (cLat != null) "Location: Fixed 📍" else "Location: Pending", style = MaterialTheme.typography.bodySmall, color = if (cLat != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
            Button(onClick = { resolveAddress(cRawPaste, false) }, enabled = cRawPaste.isNotBlank()) { Text("Verify") }
        }

        HorizontalDivider()

        Text("Dispatched Miles", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = dBounce,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() || it == '.' }
                    if (filtered.count { it == '.' } <= 1) dBounce = filtered
                },
                label = { Text("Disp. Bounce *") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
            )
            OutlinedTextField(
                value = dLoaded,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() || it == '.' }
                    if (filtered.count { it == '.' } <= 1) dLoaded = filtered
                },
                label = { Text("Disp. Loaded *") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isGoingHome, onCheckedChange = { isGoingHome = it })
            Text("Going Home Load")
        }

        if (editingLoad == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isManualEntry, onCheckedChange = { isManualEntry = it })
                Text("Manual Historical Entry")
            }
        }

        if (isManualEntry) {
            Text("Historical Data", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = dateLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Pickup Date") },
                modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = matchDispatched, onCheckedChange = { matchDispatched = it })
                Text("Match Dispatched Miles")
            }
            if (!matchDispatched) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = manualActBounce, onValueChange = { manualActBounce = it.filter { it.isDigit() || it == '.' } }, label = { Text("Actual Bounce") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = manualActLoaded, onValueChange = { manualActLoaded = it.filter { it.isDigit() || it == '.' } }, label = { Text("Actual Loaded") }, modifier = Modifier.weight(1f))
                }
            }
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        selectedDateMillis = datePickerState.selectedDateMillis ?: selectedDateMillis
                        showDatePicker = false
                    }) { Text("OK") }
                }
            ) { DatePicker(state = datePickerState) }
        }

        HorizontalDivider()

        Text("Revenue Details", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = ratePct, onValueChange = { ratePct = it.filter { it.isDigit() || it == '.' } }, label = { Text("Pay Split (%)") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = loadPay, onValueChange = { loadPay = it.filter { it.isDigit() || it == '.' } }, label = { Text("Gross Pay ($)") }, modifier = Modifier.weight(1f))
        }

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(value = selectedTarp, onValueChange = {}, readOnly = true, label = { Text("Tarp Type") }, modifier = Modifier.fillMaxWidth())
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                tarpOpts.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = { selectedTarp = option; expanded = false })
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isPreTarped, onCheckedChange = { isPreTarped = it })
            Text("Load Pre-Tarped")
        }

        OutlinedTextField(value = tripNotes, onValueChange = { tripNotes = it }, label = { Text("Trip Notes (Optional)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)

        HorizontalDivider()

        val isFormValid = proNum.trim().isNotEmpty() && loadPay.trim().isNotEmpty() && sLat != null && cLat != null && dBounce.trim().isNotEmpty() && dLoaded.trim().isNotEmpty()

        Button(
            onClick = {
                val tChar = when(selectedTarp) {
                    "8' Drop" -> "L"
                    "4' Drop" -> "S"
                    else -> "N"
                }
                val dispB = dBounce.toDoubleOrNull() ?: 0.0
                val dispL = dLoaded.toDoubleOrNull() ?: 0.0

                if (isManualEntry) {
                    val manualData = CurrentLoad(
                        proNumber = proNum.trim(),
                        dispatchedBounceMiles = dispB,
                        dispatchedLoadedMiles = dispL,
                        bounceMilesStart = 0.0,
                        bounceMilesEnd = if (matchDispatched) dispB else manualActBounce.toDoubleOrNull() ?: 0.0,
                        loadedMilesStart = 0.0,
                        loadedMilesEnd = if (matchDispatched) dispL else manualActLoaded.toDoubleOrNull() ?: 0.0,
                        percentageRate = ratePct.toDoubleOrNull() ?: 31.0,
                        loadPay = loadPay.toDoubleOrNull() ?: 0.0,
                        tarpType = tChar,
                        isPreTarped = isPreTarped,
                        isGoingHome = isGoingHome,
                        pickupTimestamp = selectedDateMillis,
                        tripState = "COMPLETED",
                        tripNotes = tripNotes.ifBlank { null },
                        deliveryTimestamp = selectedDateMillis + 3600000,
                        shipperName = sName,
                        shipperLat = sLat,
                        shipperLong = sLong,
                        consigneeName = cName,
                        consigneeLat = cLat,
                        consigneeLong = cLong
                    )
                    processSave(manualData)
                } else {
                    val draftData = CurrentLoad(
                        proNumber = proNum.trim(),
                        dispatchedBounceMiles = dispB,
                        dispatchedLoadedMiles = dispL,
                        bounceMilesStart = 0.0,
                        bounceMilesEnd = 0.0,
                        loadedMilesStart = 0.0,
                        loadedMilesEnd = 0.0,
                        percentageRate = ratePct.toDoubleOrNull() ?: 31.0,
                        loadPay = loadPay.toDoubleOrNull() ?: 0.0,
                        tarpType = tChar,
                        isPreTarped = isPreTarped,
                        isGoingHome = isGoingHome,
                        pickupTimestamp = System.currentTimeMillis(),
                        tripState = "ACTIVE_BOUNCE",
                        tripNotes = tripNotes.ifBlank { null },
                        shipperName = sName,
                        shipperLat = sLat,
                        shipperLong = sLong,
                        consigneeName = cName,
                        consigneeLat = cLat,
                        consigneeLong = cLong
                    )
                    TrackingService.targetLat = sLat
                    TrackingService.targetLong = sLong
                    TrackingService.targetName = sName
                    TrackingService.isGeofenceActive = true
                    ctx.startService(Intent(ctx, TrackingService::class.java))
                    processSave(draftData)
                }
            },
            enabled = isFormValid,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text(if (isManualEntry) "Save Record" else "Start Journey")
        }

        TextButton(onClick = onCancelClick, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }

    if (showFridayReminder && pendingLoadSave != null) {
        AlertDialog(
            onDismissRequest = { showFridayReminder = false },
            title = { Text("Friday Home Run?") },
            text = { Text("It's Friday! Is this a 'Going Home' load? Marking it as such will protect your OOR percentage.") },
            confirmButton = {
                Button(onClick = {
                    onSaveClick(pendingLoadSave!!.copy(isGoingHome = true))
                    showFridayReminder = false
                }) { Text("Yes, Home Run") }
            },
            dismissButton = {
                TextButton(onClick = {
                    onSaveClick(pendingLoadSave!!)
                    showFridayReminder = false
                }) { Text("No, Work Only") }
            }
        )
    }
}
