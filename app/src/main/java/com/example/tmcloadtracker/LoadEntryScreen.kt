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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadEntryScreen(
    initialPercentage: String,
    editingLoad: CurrentLoad? = null,
    onSaveClick: (CurrentLoad) -> Unit,
    onCancelClick: () -> Unit,
) {
    val ctx = LocalContext.current
    val scroll = rememberScrollState()

    var proNum by remember { mutableStateOf(value = editingLoad?.proNumber ?: "") }
    var dBounce by remember { mutableStateOf(value = editingLoad?.dispatchedBounceMiles?.toString() ?: "") }
    var dLoaded by remember { mutableStateOf(value = editingLoad?.dispatchedLoadedMiles?.toString() ?: "") }
    var ratePct by remember { mutableStateOf(value = editingLoad?.percentageRate?.toString() ?: initialPercentage) }
    var loadPay by remember { mutableStateOf(value = editingLoad?.loadPay?.toString() ?: "") }

    var expanded by remember { mutableStateOf(value = false) }
    val tarpOpts = listOf("None", "4' Drop", "8' Drop")
    var selectedTarp by remember {
        val type = editingLoad?.tarpType ?: "N"
        mutableStateOf(
            value = when (type) {
                "S" -> "4' Drop"
                "L" -> "8' Drop"
                else -> "None"
            },
        )
    }

    var isPreTarped by remember { mutableStateOf(value = editingLoad?.isPreTarped ?: false) }
    var isGoingHome by remember { mutableStateOf(value = editingLoad?.isGoingHome ?: false) }
    var tripNotes by remember { mutableStateOf(value = editingLoad?.tripNotes ?: "") }
    var isManualEntry by remember { mutableStateOf(value = false) }

    var manualActBounce by remember { mutableStateOf(value = "") }
    var manualActLoaded by remember { mutableStateOf(value = "") }
    var matchDispatched by remember { mutableStateOf(value = true) }

    var showDatePicker by remember { mutableStateOf(value = false) }
    var selectedDateMillis by remember { mutableLongStateOf(value = System.currentTimeMillis()) }
    val dateLabel = remember(key1 = selectedDateMillis) {
        val instant = Instant.ofEpochMilli(selectedDateMillis)
        val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
        // 📍 FIX: DatePicker returns UTC. Use ZoneOffset.UTC to prevent day-shifting in local time zones.
        instant.atZone(ZoneOffset.UTC).toLocalDate().format(formatter)
    }

    var sRawPaste by remember { mutableStateOf(value = "") }
    var sName by remember { mutableStateOf(value = editingLoad?.shipperName ?: "") }
    var sLat by remember { mutableStateOf(value = editingLoad?.shipperLat) }
    var sLong by remember { mutableStateOf(value = editingLoad?.shipperLong) }

    var cRawPaste by remember { mutableStateOf(value = "") }
    var cName by remember { mutableStateOf(value = editingLoad?.consigneeName ?: "") }
    var cLat by remember { mutableStateOf(value = editingLoad?.consigneeLat) }
    var cLong by remember { mutableStateOf(value = editingLoad?.consigneeLong) }

    fun resolveAddress(input: String, isShipper: Boolean) {
        val lines = input.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return

        val name = lines[0].trim()
        val addressPart = if (lines.size > 1) {
            lines.asSequence().drop(n = 1).joinToString(separator = ", ").trim()
        } else {
            name
        }

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

    var showFridayReminder by remember { mutableStateOf(value = false) }
    var pendingLoadSave by remember { mutableStateOf<CurrentLoad?>(value = null) }

    fun processSave(load: CurrentLoad) {
        val dateToCheck = Instant.ofEpochMilli(load.pickupTimestamp)
            .atZone(ZoneOffset.UTC).toLocalDate()

        if (!isManualEntry && (dateToCheck.dayOfWeek == DayOfWeek.FRIDAY) && !load.isGoingHome) {
            pendingLoadSave = load
            showFridayReminder = true
        } else {
            onSaveClick(load)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(all = 16.dp)
            .verticalScroll(state = scroll),
        verticalArrangement = Arrangement.spacedBy(space = 16.dp),
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
            onValueChange = { input -> sRawPaste = input },
            label = { Text("Paste Shipper Name & Address") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(weight = 1f)) {
                Text(text = if (sName.isNotEmpty()) "Name: $sName" else "Name: Not Parsed", style = MaterialTheme.typography.bodySmall)
                Text(text = if (sLat != null) "Location: Fixed 📍" else "Location: Pending", style = MaterialTheme.typography.bodySmall, color = if (sLat != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = { resolveAddress(input = sRawPaste, isShipper = true) },
                enabled = sRawPaste.isNotBlank(),
            ) {
                Text("Verify")
            }
        }

        HorizontalDivider()

        Text("Consignee Details (Required)", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = cRawPaste,
            onValueChange = { input -> cRawPaste = input },
            label = { Text("Paste Consignee Name & Address") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(weight = 1f)) {
                Text(text = if (cName.isNotEmpty()) "Name: $cName" else "Name: Not Parsed", style = MaterialTheme.typography.bodySmall)
                Text(text = if (cLat != null) "Location: Fixed 📍" else "Location: Pending", style = MaterialTheme.typography.bodySmall, color = if (cLat != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = { resolveAddress(input = cRawPaste, isShipper = false) },
                enabled = cRawPaste.isNotBlank(),
            ) {
                Text("Verify")
            }
        }

        HorizontalDivider()

        Text("Dispatched Miles", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(space = 8.dp)) {
            OutlinedTextField(
                value = dBounce,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() || (it == '.') }
                    if (filtered.count { it == '.' } <= 1) dBounce = filtered
                },
                label = { Text("Disp. Bounce *") },
                modifier = Modifier.weight(weight = 1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
            )
            OutlinedTextField(
                value = dLoaded,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() || (it == '.') }
                    if (filtered.count { it == '.' } <= 1) dLoaded = filtered
                },
                label = { Text("Disp. Loaded *") },
                modifier = Modifier.weight(weight = 1f),
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
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = matchDispatched, onCheckedChange = { matchDispatched = it })
                Text("Match Dispatched Miles")
            }
            if (!matchDispatched) {
                Row(horizontalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                    OutlinedTextField(
                        value = manualActBounce,
                        onValueChange = { input -> manualActBounce = input.filter { it.isDigit() || (it == '.') } },
                        label = { Text("Actual Bounce") },
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = manualActLoaded,
                        onValueChange = { input -> manualActLoaded = input.filter { it.isDigit() || (it == '.') } },
                        label = { Text("Actual Loaded") },
                        modifier = Modifier.weight(1f),
                    )
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
        Row(horizontalArrangement = Arrangement.spacedBy(space = 8.dp)) {
            OutlinedTextField(value = ratePct, onValueChange = { input -> ratePct = input.filter { it.isDigit() || (it == '.') } }, label = { Text("Pay Split (%)") }, modifier = Modifier.weight(weight = 1f))
            OutlinedTextField(value = loadPay, onValueChange = { input -> loadPay = input.filter { it.isDigit() || (it == '.') } }, label = { Text("Gross Pay ($)") }, modifier = Modifier.weight(weight = 1f))
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedTarp,
                onValueChange = {},
                readOnly = true,
                label = { Text("Tarp Type") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                tarpOpts.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text = option) },
                        onClick = {
                            selectedTarp = option
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isPreTarped, onCheckedChange = { isPreTarped = it })
            Text("Load Pre-Tarped")
        }

        OutlinedTextField(value = tripNotes, onValueChange = { input -> tripNotes = input }, label = { Text("Trip Notes (Optional)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)

        HorizontalDivider()

        val isFormValid = (proNum.trim().isNotEmpty()) && (loadPay.trim().isNotEmpty()) && (sLat != null) && (cLat != null) && (dBounce.trim().isNotEmpty()) && (dLoaded.trim().isNotEmpty())

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
            modifier = Modifier.fillMaxWidth().height(height = 50.dp)
        ) {
            Text(if (isManualEntry) "Save Record" else "Start Journey")
        }

        TextButton(
            onClick = { onCancelClick() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Cancel Load Entry")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
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
