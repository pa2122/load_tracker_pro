package com.example.tmcloadtracker

import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadEntryScreen(
    initialPercentage: String,
    initialIsTraining: Boolean = false,
    initialTrainerPayRate: Double = 200.0,
    isProUser: Boolean = true,
    editingLoad: CurrentLoad? = null,
    isHistorical: Boolean = false,
    existingProNumbers: List<String> = emptyList(),
    onSaveClick: (CurrentLoad) -> Unit,
    onCancelClick: () -> Unit,
) {
    val ctx = LocalContext.current
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()

    var proNum by remember { mutableStateOf(value = editingLoad?.proNumber ?: "") }
    var showDuplicateWarningDialog by remember { mutableStateOf(false) }
    var pendingDuplicateLoad by remember { mutableStateOf<CurrentLoad?>(null) }
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
    var isTrainingWeek by remember { mutableStateOf(value = editingLoad?.isTrainingWeek ?: initialIsTraining) }
    var tripNotes by remember { mutableStateOf(value = editingLoad?.tripNotes ?: "") }
    var isManualEntry by remember { mutableStateOf(value = false) }

    var manualActBounce by remember { mutableStateOf(editingLoad?.bounceMilesEnd?.let { if (it > 0) String.format(Locale.US, "%.1f", it) else "" } ?: "") }
    var manualActLoaded by remember { mutableStateOf(editingLoad?.loadedMilesEnd?.let { if (it > 0) String.format(Locale.US, "%.1f", it) else "" } ?: "") }
    var matchDispatched by remember { mutableStateOf(editingLoad == null || (editingLoad.bounceMilesEnd == 0.0 && editingLoad.loadedMilesEnd == 0.0)) }

    var showDatePicker by remember { mutableStateOf(value = false) }
    var showProPaywallDialog by remember { mutableStateOf(value = false) }
    var selectedDateMillis by remember { mutableLongStateOf(value = editingLoad?.pickupTimestamp ?: System.currentTimeMillis()) }
    val dateLabel = remember(key1 = selectedDateMillis) {
        val instant = Instant.ofEpochMilli(selectedDateMillis)
        val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
        instant.atZone(ZoneOffset.UTC).toLocalDate().format(formatter)
    }

    var sRawPaste by remember { mutableStateOf(value = editingLoad?.shipperName ?: "") }
    var sName by remember { mutableStateOf(value = editingLoad?.shipperName?.lines()?.firstOrNull()?.trim() ?: "") }
    var sLat by remember { mutableStateOf(value = editingLoad?.shipperLat) }
    var sLong by remember { mutableStateOf(value = editingLoad?.shipperLong) }
    var isShipperVerified by remember { mutableStateOf(value = editingLoad?.shipperLat != null) }

    var cRawPaste by remember { mutableStateOf(value = editingLoad?.consigneeName ?: "") }
    var cName by remember { mutableStateOf(value = editingLoad?.consigneeName?.lines()?.firstOrNull()?.trim() ?: "") }
    var cLat by remember { mutableStateOf(value = editingLoad?.consigneeLat) }
    var cLong by remember { mutableStateOf(value = editingLoad?.consigneeLong) }
    var isConsigneeVerified by remember { mutableStateOf(value = editingLoad?.consigneeLat != null) }

    var sApptText by remember { mutableStateOf(value = editingLoad?.pickupApptText) }
    var sApptTimestamp by remember { mutableStateOf(value = editingLoad?.pickupApptTimestamp) }
    var sApptType by remember { mutableStateOf(value = editingLoad?.pickupApptType) }

    var cApptText by remember { mutableStateOf(value = editingLoad?.consigneeApptText) }
    var cApptTimestamp by remember { mutableStateOf(value = editingLoad?.consigneeApptTimestamp) }
    var cApptType by remember { mutableStateOf(value = editingLoad?.consigneeApptType) }

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

        scope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(ctx, Locale.US)
                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocationName(addressPart, 1)
                withContext(Dispatchers.Main) {
                    if (!results.isNullOrEmpty()) {
                        val loc = results[0]
                        if (isShipper) {
                            sLat = loc.latitude
                            sLong = loc.longitude
                            isShipperVerified = true
                        } else {
                            cLat = loc.latitude
                            cLong = loc.longitude
                            isConsigneeVerified = true
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    var showFridayReminder by remember { mutableStateOf(value = false) }
    var pendingLoadSave by remember { mutableStateOf<CurrentLoad?>(value = null) }

    fun parseApptDateToMillis(dateStr: String): Long? {
        val patterns = listOf(
            "M/d/yyyy h:mm:ss a",
            "M/d/yyyy h:mm a",
            "MM/dd/yyyy HH:mm:ss",
            "MM/dd/yyyy HH:mm"
        )
        for (pattern in patterns) {
            try {
                val formatter = DateTimeFormatter.ofPattern(pattern, Locale.US)
                val ldt = LocalDateTime.parse(dateStr, formatter)
                return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (_: Exception) {}
        }
        return null
    }

    fun processOcrText(rawText: String) {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

        // 1. Extract PRO #
        val proRegex = Regex("""\b(?:PRO|Order|Load|Trip)\s*(?:Number|No|Num)?\s*#?\s*:?\s*(\d{4,12})""", RegexOption.IGNORE_CASE)
        val proMatch = proRegex.find(rawText)
        if (proMatch != null && proMatch.groupValues.size > 1) {
            proNum = proMatch.groupValues[1]
        } else {
            val fallbackPro = Regex("""\b\d{6,10}\b""").find(rawText)
            if (fallbackPro != null) proNum = fallbackPro.value
        }

        // 2. Extract Load Pay
        val payRegex = Regex(
            """(?:Pay|Gross|Rate|Linehaul|Total|Amount)\D*?\$?\s*(\d{1,3}(?:,\d{3})+|\d+)(?:\.(\d{1,2}))?""",
            RegexOption.IGNORE_CASE
        )
        val payMatch = payRegex.find(rawText)
        if (payMatch != null && payMatch.groupValues.size > 1) {
            val intPart = payMatch.groupValues[1].replace(",", "")
            val decPart = if (payMatch.groupValues.size > 2 && payMatch.groupValues[2].isNotBlank()) "." + payMatch.groupValues[2] else ""
            loadPay = intPart + decPart
        } else {
            val dollarMatch = Regex("""\$?\s*([1-9][0-9]{2,4}(?:\.[0-9]{2})?)""").find(rawText)
            if (dollarMatch != null && dollarMatch.groupValues.size > 1) {
                loadPay = dollarMatch.groupValues[1].replace(",", "")
            }
        }

        // 3. Extract Dispatched Miles (Loaded Miles & Bounce/Deadhead Miles)
        val numRegex = Regex("""\b\d{1,4}(?:,\d{3})*(?:\.\d+)?\b""")
        var extractedLoaded: String? = null
        var extractedBounce: String? = null

        // Direct regex check for Loaded Miles & Bounce Miles
        val loadedRegex = Regex("""(?:loaded\s*miles|loaded\s*mi|load\s*miles)\s*[:=\-\s]*(\d{1,4}(?:,\d{3})*(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
        val loadedMatch = loadedRegex.find(rawText)
        if (loadedMatch != null && loadedMatch.groupValues.size > 1) {
            extractedLoaded = loadedMatch.groupValues[1].replace(",", "")
        }

        val bounceRegex = Regex("""(?:bounce\s*miles|bounce\s*mi|deadhead\s*miles|deadhead\s*mi|dh\s*miles)\s*[:=\-\s]*(\d{1,4}(?:,\d{3})*(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
        val bounceMatch = bounceRegex.find(rawText)
        if (bounceMatch != null && bounceMatch.groupValues.size > 1) {
            extractedBounce = bounceMatch.groupValues[1].replace(",", "")
        }

        // Line-by-line inspection fallback
        if (extractedLoaded == null || extractedBounce == null) {
            for (i in lines.indices) {
                val line = lines[i]
                val lower = line.lowercase(Locale.US)

                if (extractedLoaded == null && (lower.contains("loaded miles") || lower.contains("loaded mi") || lower.contains("loaded:"))) {
                    val match = numRegex.find(line)
                    if (match != null) extractedLoaded = match.value.replace(",", "")
                }

                if (extractedBounce == null && (lower.contains("bounce miles") || lower.contains("bounce mi") || lower.contains("deadhead miles") || lower.contains("dh miles"))) {
                    val match = numRegex.find(line)
                    if (match != null) extractedBounce = match.value.replace(",", "")
                }
            }
        }

        if (extractedLoaded != null) dLoaded = extractedLoaded
        if (extractedBounce != null) dBounce = extractedBounce

        // 4. Extract Shipper & Consignee
        var foundShipper = false
        var foundConsignee = false
        val shipperSb = StringBuilder()
        val consigneeSb = StringBuilder()

        for (i in lines.indices) {
            val line = lines[i]
            val lower = line.lowercase(Locale.US)
            if (lower.contains("shipper") || lower.contains("pickup") || lower.contains("origin")) {
                foundShipper = true
                foundConsignee = false
                val name = line.substringAfter(":").trim()
                if (name.isNotBlank() && !name.equals("origin", ignoreCase = true) && !name.equals("pickup", ignoreCase = true) && !name.equals("shipper", ignoreCase = true)) {
                    shipperSb.append(name).append("\n")
                }
                continue
            }
            if (lower.contains("consignee") || lower.contains("delivery") || lower.contains("destination") || lower.contains("drop")) {
                foundConsignee = true
                foundShipper = false
                val name = line.substringAfter(":").trim()
                if (name.isNotBlank() && !name.equals("final dropoff", ignoreCase = true) && !name.equals("dropoff", ignoreCase = true) && !name.equals("drop", ignoreCase = true) && !name.equals("consignee", ignoreCase = true) && !name.equals("delivery", ignoreCase = true)) {
                    consigneeSb.append(name).append("\n")
                }
                continue
            }
            if (foundShipper && shipperSb.lines().size < 4) {
                shipperSb.append(line).append("\n")
            }
            if (foundConsignee && consigneeSb.lines().size < 4) {
                consigneeSb.append(line).append("\n")
            }
        }

        if (shipperSb.isNotBlank()) {
            sRawPaste = shipperSb.toString().trim()
            resolveAddress(sRawPaste, isShipper = true)
        }
        if (consigneeSb.isNotBlank()) {
            cRawPaste = consigneeSb.toString().trim()
            resolveAddress(cRawPaste, isShipper = false)
        }

        // 5. Extract Going Home status
        val homeMatch = Regex("""(?:Going\s*Home|Home\s*Run|Home)\s*[:=\-\s]*([YN])""", RegexOption.IGNORE_CASE).find(rawText)
        if (homeMatch != null && homeMatch.groupValues.size > 1) {
            isGoingHome = homeMatch.groupValues[1].equals("Y", ignoreCase = true)
        }

        // 6. Extract Tarp type
        val tarpMatch = Regex("""Tarp\s*[:=\-\s]*([SLN])""", RegexOption.IGNORE_CASE).find(rawText)
        if (tarpMatch != null && tarpMatch.groupValues.size > 1) {
            selectedTarp = when (tarpMatch.groupValues[1].uppercase(Locale.US)) {
                "S" -> "4' Drop"
                "L" -> "8' Drop"
                else -> "None"
            }
        }

        // 7. Extract Optional Facility Instructions, Directions & Parking Info
        var currentFacility = ""
        val shipperInst = StringBuilder()
        val shipperDir = StringBuilder()
        var shipperPark = ""

        val consigneeInst = StringBuilder()
        val consigneeDir = StringBuilder()
        var consigneePark = ""

        var currentField = ""

        for (i in lines.indices) {
            val line = lines[i]
            val lower = line.lowercase(Locale.US)

            if (lower.contains("shipper") || lower.contains("pickup") || lower.contains("origin")) {
                currentFacility = "shipper"
                currentField = ""
            } else if (lower.contains("consignee") || lower.contains("delivery") || lower.contains("destination") || lower.contains("drop")) {
                currentFacility = "consignee"
                currentField = ""
            } else if (lower.contains("pro number") || lower.contains("load number") || lower.contains("gross:") || lower.contains("additional load notes")) {
                currentFacility = ""
                currentField = ""
            }

            if (lower.startsWith("instructions:") || lower.startsWith("instruction:")) {
                currentField = "instructions"
                val content = line.substringAfter(":").trim()
                if (content.isNotBlank()) {
                    if (currentFacility == "shipper") shipperInst.append(content).append(" ")
                    else if (currentFacility == "consignee") consigneeInst.append(content).append(" ")
                }
                continue
            }

            if (lower.startsWith("directions:") || lower.startsWith("direction:")) {
                currentField = "directions"
                val content = line.substringAfter(":").trim()
                if (content.isNotBlank()) {
                    if (currentFacility == "shipper") shipperDir.append(content).append(" ")
                    else if (currentFacility == "consignee") consigneeDir.append(content).append(" ")
                }
                continue
            }

            if (lower.startsWith("parking:")) {
                currentField = "parking"
                val content = line.substringAfter(":").trim()
                if (currentFacility == "shipper") shipperPark = content
                else if (currentFacility == "consignee") consigneePark = content
                continue
            }

            if (currentField == "instructions" && !line.contains(":")) {
                if (currentFacility == "shipper") shipperInst.append(line).append(" ")
                else if (currentFacility == "consignee") consigneeInst.append(line).append(" ")
            } else if (currentField == "directions" && !line.contains(":")) {
                if (currentFacility == "shipper") shipperDir.append(line).append(" ")
                else if (currentFacility == "consignee") consigneeDir.append(line).append(" ")
            }
        }

        val notesSb = StringBuilder()
        if (shipperInst.isNotBlank() || shipperDir.isNotBlank() || shipperPark.isNotBlank()) {
            notesSb.append("--- SHIPPER FACILITY NOTES ---\n")
            if (shipperInst.isNotBlank()) notesSb.append("⚠️ Instructions: ").append(shipperInst.toString().trim()).append("\n")
            if (shipperDir.isNotBlank()) notesSb.append("🗺️ Directions: ").append(shipperDir.toString().trim()).append("\n")
            if (shipperPark.isNotBlank()) notesSb.append("🅿️ Parking: ").append(shipperPark).append("\n")
            notesSb.append("\n")
        }

        if (consigneeInst.isNotBlank() || consigneeDir.isNotBlank() || consigneePark.isNotBlank()) {
            notesSb.append("--- CONSIGNEE FACILITY NOTES ---\n")
            if (consigneeInst.isNotBlank()) notesSb.append("⚠️ Instructions: ").append(consigneeInst.toString().trim()).append("\n")
            if (consigneeDir.isNotBlank()) notesSb.append("🗺️ Directions: ").append(consigneeDir.toString().trim()).append("\n")
            if (consigneePark.isNotBlank()) notesSb.append("🅿️ Parking: ").append(consigneePark).append("\n")
        }

        if (notesSb.isNotBlank()) {
            tripNotes = notesSb.toString().trim()
        }

        // 8. Extract Appointment Date/Time Lines (@ or B)
        val apptRegex = Regex("""(?:[@B]\s*)?(\d{1,2}/\d{1,2}/\d{2,4}\s+\d{1,2}:\d{2}(?::\d{2})?\s*(?:AM|PM)?)""", RegexOption.IGNORE_CASE)
        for (i in lines.indices) {
            val line = lines[i]
            val match = apptRegex.find(line)
            if (match != null) {
                val apptText = line.trim()
                val isBeforeType = apptText.startsWith("B", ignoreCase = true)
                val type = if (isBeforeType) "BEFORE" else "EXACT"
                val rawDateStr = match.groupValues[1].trim()
                val parsedTs = parseApptDateToMillis(rawDateStr)

                if (i < lines.size / 2 && sApptTimestamp == null) {
                    sApptText = apptText
                    sApptTimestamp = parsedTs
                    sApptType = type
                    if (parsedTs != null) {
                        selectedDateMillis = parsedTs
                    }
                } else if (cApptTimestamp == null) {
                    cApptText = apptText
                    cApptTimestamp = parsedTs
                    cApptType = type
                }
            }
        }
    }

    var isScanningOcr by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isScanningOcr = true
            try {
                val inputImage = InputImage.fromFilePath(ctx, uri)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        isScanningOcr = false
                        val text = visionText.text
                        if (text.isNotBlank()) {
                            processOcrText(text)
                            Toast.makeText(ctx, "Scan complete! Auto-filled fields from screenshot.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(ctx, "No text detected in selected image.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        isScanningOcr = false
                        Toast.makeText(ctx, "Failed to scan image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                    .addOnCompleteListener {
                        recognizer.close()
                    }
            } catch (e: Exception) {
                isScanningOcr = false
                Toast.makeText(ctx, "Error reading image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun processSave(loadToSave: CurrentLoad) {
        if (editingLoad == null && existingProNumbers.contains(loadToSave.proNumber)) {
            pendingDuplicateLoad = loadToSave
            showDuplicateWarningDialog = true
            return
        }

        val dateToCheck = Instant.ofEpochMilli(loadToSave.pickupTimestamp)
            .atZone(ZoneId.systemDefault()).toLocalDate()

        if (!isManualEntry && (dateToCheck.dayOfWeek == DayOfWeek.FRIDAY) && !loadToSave.isGoingHome) {
            pendingLoadSave = loadToSave
            showFridayReminder = true
        } else {
            onSaveClick(loadToSave)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(all = 16.dp)
            .verticalScroll(state = scroll),
        verticalArrangement = Arrangement.spacedBy(space = 16.dp),
    ) {
        Text(if (editingLoad == null) (if (isHistorical) "New Historical Completed Load" else "New Freight Load") else "Edit Freight Load", style = MaterialTheme.typography.headlineMedium)

        if (editingLoad == null || isHistorical) {
            Button(
                onClick = {
                    if (isProUser) {
                        imagePickerLauncher.launch("image/*")
                    } else {
                        showProPaywallDialog = true
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isScanningOcr) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scanning Screenshot...")
                } else {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isProUser) "📷 Auto-Fill from Screenshot (Pro)" else "📷 Auto-Fill from Screenshot (Pro Locked)")
                }
            }
        }

        OutlinedTextField(
            value = proNum,
            onValueChange = { input -> proNum = input.filter { it.isDigit() } },
            label = { Text("PRO # (Required)") },
            singleLine = true,
            enabled = editingLoad == null || isHistorical,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider()

        Text("Shipper Details (Required)", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = sRawPaste,
            onValueChange = { input ->
                sRawPaste = input
                isShipperVerified = false
                sLat = null
                sLong = null
            },
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
                Text(text = if (isShipperVerified) "Location: Fixed 📍" else "Location: Pending", style = MaterialTheme.typography.bodySmall, color = if (isShipperVerified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = { resolveAddress(input = sRawPaste, isShipper = true) },
                enabled = sRawPaste.isNotBlank() && !isShipperVerified,
            ) {
                Text(if (isShipperVerified) "Verified ✅" else "Verify")
            }
        }

        HorizontalDivider()

        Text("Consignee Details (Required)", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = cRawPaste,
            onValueChange = { input ->
                cRawPaste = input
                isConsigneeVerified = false
                cLat = null
                cLong = null
            },
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
                Text(text = if (isConsigneeVerified) "Location: Fixed 📍" else "Location: Pending", style = MaterialTheme.typography.bodySmall, color = if (isConsigneeVerified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = { resolveAddress(input = cRawPaste, isShipper = false) },
                enabled = cRawPaste.isNotBlank() && !isConsigneeVerified,
            ) {
                Text(if (isConsigneeVerified) "Verified ✅" else "Verify")
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
            )
            OutlinedTextField(
                value = dLoaded,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() || (it == '.') }
                    if (filtered.count { it == '.' } <= 1) dLoaded = filtered
                },
                label = { Text("Disp. Loaded *") },
                modifier = Modifier.weight(weight = 1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isGoingHome, onCheckedChange = { isGoingHome = it })
            Text("Going Home Load")
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isTrainingWeek, onCheckedChange = { isTrainingWeek = it })
            Text("Training Week Load (Adds Weekly Bonus)")
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
                        val picked = datePickerState.selectedDateMillis
                        if (picked != null) {
                            selectedDateMillis = picked + (12 * 3600 * 1000L)
                        }
                        showDatePicker = false
                    }) { Text("OK") }
                }
            ) { DatePicker(state = datePickerState) }
        }

        HorizontalDivider()

        Text("Revenue Details", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(space = 8.dp)) {
            OutlinedTextField(
                value = ratePct,
                onValueChange = { input -> ratePct = input.filter { it.isDigit() || (it == '.') } },
                label = { Text("Pay Split (%)") },
                modifier = Modifier.weight(weight = 1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
            )
            OutlinedTextField(
                value = loadPay,
                onValueChange = { input -> loadPay = input.filter { it.isDigit() || (it == '.') } },
                label = { Text("Gross Pay ($)") },
                modifier = Modifier.weight(weight = 1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done)
            )
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

        val isFormValid = proNum.trim().isNotEmpty() &&
                loadPay.toDoubleOrNull() != null &&
                dBounce.toDoubleOrNull() != null &&
                dLoaded.toDoubleOrNull() != null &&
                sLat != null &&
                cLat != null

        fun processSave(loadToSave: CurrentLoad) {
            if (editingLoad == null && existingProNumbers.contains(loadToSave.proNumber)) {
                pendingDuplicateLoad = loadToSave
                showDuplicateWarningDialog = true
            } else {
                onSaveClick(loadToSave)
            }
        }

        Button(
            onClick = {
                val tChar = when(selectedTarp) {
                    "8' Drop" -> "L"
                    "4' Drop" -> "S"
                    else -> "N"
                }
                val dispB = dBounce.toDoubleOrNull() ?: 0.0
                val dispL = dLoaded.toDoubleOrNull() ?: 0.0

                val resolvedTrainerRate = if (isTrainingWeek) {
                    if (editingLoad != null && editingLoad.trainerPayRate > 0.0) {
                        editingLoad.trainerPayRate
                    } else {
                        initialTrainerPayRate
                    }
                } else {
                    0.0
                }

                if (isManualEntry) {
                    val manualData = CurrentLoad(
                        proNumber = proNum.trim(),
                        dispatchedBounceMiles = dispB,
                        dispatchedLoadedMiles = dispL,
                        bounceMilesStart = 0.0,
                        bounceMilesEnd = if (matchDispatched) dispB else manualActBounce.toDoubleOrNull() ?: 0.0,
                        loadedMilesStart = 0.0,
                        loadedMilesEnd = if (matchDispatched) dispL else manualActLoaded.toDoubleOrNull() ?: 0.0,
                        percentageRate = ratePct.toDoubleOrNull() ?: initialPercentage.toDoubleOrNull() ?: 31.0,
                        loadPay = loadPay.toDoubleOrNull() ?: 0.0,
                        tarpType = tChar,
                        isPreTarped = isPreTarped,
                        isGoingHome = isGoingHome,
                        isTrainingWeek = isTrainingWeek,
                        trainerPayRate = resolvedTrainerRate,
                        pickupTimestamp = selectedDateMillis,
                        tripState = "COMPLETED",
                        tripNotes = tripNotes.ifBlank { null },
                        deliveryTimestamp = selectedDateMillis + 3600000,
                        shipperName = if (sRawPaste.isNotBlank()) sRawPaste.trim() else sName,
                        shipperLat = sLat,
                        shipperLong = sLong,
                        consigneeName = if (cRawPaste.isNotBlank()) cRawPaste.trim() else cName,
                        consigneeLat = cLat,
                        consigneeLong = cLong,
                        pickupApptText = sApptText,
                        pickupApptTimestamp = sApptTimestamp,
                        pickupApptType = sApptType,
                        consigneeApptText = cApptText,
                        consigneeApptTimestamp = cApptTimestamp,
                        consigneeApptType = cApptType
                    )
                    processSave(manualData)
                } else {
                    val actB = if (matchDispatched) 0.0 else (manualActBounce.toDoubleOrNull() ?: 0.0)
                    val actL = if (matchDispatched) 0.0 else (manualActLoaded.toDoubleOrNull() ?: 0.0)

                    val draftData = CurrentLoad(
                        proNumber = proNum.trim(),
                        dispatchedBounceMiles = dispB,
                        dispatchedLoadedMiles = dispL,
                        bounceMilesStart = 0.0,
                        bounceMilesEnd = actB,
                        loadedMilesStart = 0.0,
                        loadedMilesEnd = actL,
                        percentageRate = ratePct.toDoubleOrNull() ?: initialPercentage.toDoubleOrNull() ?: 31.0,
                        loadPay = loadPay.toDoubleOrNull() ?: 0.0,
                        tarpType = tChar,
                        isPreTarped = isPreTarped,
                        isGoingHome = isGoingHome,
                        isTrainingWeek = isTrainingWeek,
                        trainerPayRate = resolvedTrainerRate,
                        pickupTimestamp = System.currentTimeMillis(),
                        tripState = editingLoad?.tripState ?: "ACTIVE_BOUNCE",
                        tripNotes = tripNotes.ifBlank { null },
                        shipperName = if (sRawPaste.isNotBlank()) sRawPaste.trim() else sName,
                        shipperLat = sLat,
                        shipperLong = sLong,
                        consigneeName = if (cRawPaste.isNotBlank()) cRawPaste.trim() else cName,
                        consigneeLat = cLat,
                        consigneeLong = cLong,
                        pickupApptText = sApptText,
                        pickupApptTimestamp = sApptTimestamp,
                        pickupApptType = sApptType,
                        consigneeApptText = cApptText,
                        consigneeApptTimestamp = cApptTimestamp,
                        consigneeApptType = cApptType
                    )

                    if (editingLoad == null) {
                        TrackingService.resetTrackingState(ctx)
                        TrackingService.activeProNumber = proNum.trim()
                        TrackingService.targetLat = sLat
                        TrackingService.targetLong = sLong
                        TrackingService.targetName = sName
                        TrackingService.isGeofenceActive = true
                        ContextCompat.startForegroundService(ctx, Intent(ctx, TrackingService::class.java))
                    } else {
                        if (actB > 0) TrackingService.totalBounceMilesTracked.value = actB
                        if (actL > 0) TrackingService.totalLoadedMilesTracked.value = actL
                        TrackingService.saveState(ctx)
                    }
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

    val pending = pendingLoadSave
    if (showFridayReminder && pending != null) {
        AlertDialog(
            onDismissRequest = { showFridayReminder = false },
            title = { Text("Friday Home Run?") },
            text = { Text("It's Friday! Is this a 'Going Home' load? Marking it as such will protect your OOR percentage.") },
            confirmButton = {
                Button(onClick = {
                    onSaveClick(pending.copy(isGoingHome = true))
                    showFridayReminder = false
                }) { Text("Yes, Home Run") }
            },
            dismissButton = {
                TextButton(onClick = {
                    onSaveClick(pending)
                    showFridayReminder = false
                }) { Text("No, Work Only") }
            }
        )
    }

    if (showProPaywallDialog) {
        ProPaywallDialog(
            onUnlockClick = {
                showProPaywallDialog = false
                Toast.makeText(ctx, "Unlock Pro in side menu or Dev Options.", Toast.LENGTH_LONG).show()
            },
            onDismiss = { showProPaywallDialog = false }
        )
    }

    if (showDuplicateWarningDialog && pendingDuplicateLoad != null) {
        AlertDialog(
            onDismissRequest = { showDuplicateWarningDialog = false },
            title = { Text("⚠️ Duplicate PRO Number") },
            text = { Text("PRO #${pendingDuplicateLoad?.proNumber} already exists in your records. Would you like to update the existing record with these new details?") },
            confirmButton = {
                Button(onClick = {
                    onSaveClick(pendingDuplicateLoad!!)
                    showDuplicateWarningDialog = false
                }) { Text("Yes, Update") }
            },
            dismissButton = {
                TextButton(onClick = { showDuplicateWarningDialog = false }) { Text("Cancel") }
            }
        )
    }
}
