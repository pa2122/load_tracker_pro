package com.loadtracker.pro

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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
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
    var extraStopCount by remember {
        mutableStateOf(value = editingLoad?.extraStopCount?.toString() ?: "0")
    }
    val stopDrafts = remember(editingLoad) {
        mutableStateListOf<LoadStopDraft>().apply {
            addAll(
                LoadStopCodec.decode(editingLoad?.stopOffsData.orEmpty()).map { stop ->
                    LoadStopDraft(
                        name = stop.name,
                        address = stop.address,
                        latitude = stop.latitude,
                        longitude = stop.longitude,
                        dispatchedLegMiles = stop.dispatchedLegMiles.toString(),
                        actualLegMiles = stop.actualLegMiles
                    )
                }
            )
        }
    }

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
    var isManualEntry by remember(isHistorical) { mutableStateOf(value = isHistorical) }

    var manualActBounce by remember {
        mutableStateOf(
            value = if (editingLoad != null && editingLoad.bounceMilesEnd > 0.0) String.format(Locale.US, "%.1f", editingLoad.bounceMilesEnd) else ""
        )
    }
    var manualActLoaded by remember {
        mutableStateOf(
            value = if (editingLoad != null && editingLoad.loadedMilesEnd > 0.0) String.format(Locale.US, "%.1f", editingLoad.loadedMilesEnd) else ""
        )
    }
    var matchDispatched by remember { mutableStateOf(value = editingLoad == null) }

    var showDatePicker by remember { mutableStateOf(value = false) }
    var showDeliveryDatePicker by remember { mutableStateOf(value = false) }
    var showProPaywallDialog by remember { mutableStateOf(value = false) }
    var selectedDateMillis by remember(editingLoad) {
        mutableLongStateOf(value = editingLoad?.pickupTimestamp ?: System.currentTimeMillis())
    }
    var selectedDeliveryDateMillis by remember(editingLoad) {
        mutableLongStateOf(
            value = editingLoad?.deliveryTimestamp
                ?: editingLoad?.pickupTimestamp
                ?: selectedDateMillis
        )
    }
    val dateLabel = remember(key1 = selectedDateMillis) {
        val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
        Instant.ofEpochMilli(selectedDateMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(formatter)
    }
    val deliveryDateLabel = remember(key1 = selectedDeliveryDateMillis) {
        val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
        Instant.ofEpochMilli(selectedDeliveryDateMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(formatter)
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

    fun verifyStopAddress(index: Int) {
        val stop = stopDrafts.getOrNull(index) ?: return
        if (stop.address.isBlank()) {
            stopDrafts[index] = stop.copy(lookupError = "Enter a stop address before verifying.")
            return
        }
        stopDrafts[index] = stop.copy(isResolving = true, lookupError = null)
        scope.launch {
            try {
                val location = withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    Geocoder(ctx, Locale.US).getFromLocationName(stop.address, 1)
                        ?.firstOrNull()
                }
                if (index < stopDrafts.size) {
                    stopDrafts[index] = if (stopDrafts[index].address != stop.address) {
                        stopDrafts[index].copy(
                            isResolving = false,
                            lookupError = "Address changed during lookup. Verify the new address."
                        )
                    } else if (location == null) {
                        stopDrafts[index].copy(
                            isResolving = false,
                            lookupError = "Address could not be verified. Check the address and try again."
                        )
                    } else {
                        stopDrafts[index].copy(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            isResolving = false,
                            lookupError = null
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("LoadEntryScreen", "Unable to verify stop address", e)
                if (index < stopDrafts.size) {
                    stopDrafts[index] = stopDrafts[index].copy(
                        isResolving = false,
                        lookupError = "Address lookup failed. Check your connection and try again."
                    )
                }
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
        StopOffParser.parseCount(rawText)?.let { extraStopCount = it.toString() }
        if (stopDrafts.isEmpty()) {
            stopDrafts.addAll(StopOffParser.parseRouteStops(rawText))
        }
        OcrDateParser.extractDeliveryDateMillis(rawText)?.let {
            selectedDeliveryDateMillis = it
        }

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
        val loadedRegex = Regex("""(?:loaded\s*miles|loaded\s*mi|load\s*miles|trip\s*miles|distance)\s*[:=\-\s]*(\d{1,4}(?:,\d{3})*(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
        val loadedMatch = loadedRegex.find(rawText)
        if (loadedMatch != null && loadedMatch.groupValues.size > 1) {
            extractedLoaded = loadedMatch.groupValues[1].replace(",", "")
        }

        val bounceRegex = Regex("""(?:bounce\s*miles|bounce\s*mi|deadhead\s*miles|deadhead\s*mi|dh\s*miles)\s*[:=\-\s]*(\d{1,4}(?:,\d{3})*(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
        val bounceMatch = bounceRegex.find(rawText)
        if (bounceMatch != null && bounceMatch.groupValues.size > 1) {
            extractedBounce = bounceMatch.groupValues[1].replace(",", "")
        }

        // Line-by-line inspection fallback (safely ignoring headers and order numbers)
        if (extractedLoaded == null || extractedBounce == null) {
            for (line in lines) {
                val lower = line.lowercase(Locale.US)
                if (lower.contains("out of route") || lower.contains("po number") || lower.contains("order") || lower.contains("status")) {
                    continue
                }

                if (extractedLoaded == null && (lower.contains("loaded miles") || lower.contains("loaded mi"))) {
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

        // P indicates pre-tarped but doesn't identify the drop size.
        OcrTarpParser.parse(rawText)?.let { tarpInfo ->
            tarpInfo.tarpType?.let { selectedTarp = it }
            tarpInfo.isPreTarped?.let { isPreTarped = it }
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
            } else if (StopOffParser.isLabelLine(line)) {
                currentFacility = ""
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

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Toast.makeText(ctx, "Screenshot deleted from gallery", Toast.LENGTH_SHORT).show()
        }
    }

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
                        val text = OcrHelper.reconstructSpatialText(visionText)
                        if (text.isNotBlank()) {
                            processOcrText(text)
                            Toast.makeText(ctx, "Scan complete! Auto-filled fields from screenshot.", Toast.LENGTH_LONG).show()
                            OcrHelper.requestScreenshotDeletion(ctx, uri, deleteLauncher)
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

            var autoDeleteScreenshots by remember { mutableStateOf(OcrHelper.isAutoDeleteEnabled(ctx)) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Auto-delete screenshot after scan", style = MaterialTheme.typography.bodySmall)
                Switch(
                    checked = autoDeleteScreenshots,
                    onCheckedChange = {
                        autoDeleteScreenshots = it
                        OcrHelper.setAutoDeleteEnabled(ctx, it)
                    }
                )
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

        if (isManualEntry || editingLoad != null) {
            Text(if (isManualEntry) "Historical Data" else "Actual Miles Driven", style = MaterialTheme.typography.titleMedium)
            if (isManualEntry) {
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
                OutlinedTextField(
                    value = deliveryDateLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Delivery Date (verify OCR suggestion)") },
                    trailingIcon = {
                        IconButton(onClick = { showDeliveryDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Select Delivery Date")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDeliveryDatePicker = true }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = matchDispatched, onCheckedChange = { matchDispatched = it })
                    Text("Match Dispatched Miles")
                }
            }
            if (!isManualEntry || !matchDispatched) {
                Row(horizontalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                    OutlinedTextField(
                        value = manualActBounce,
                        onValueChange = { input -> manualActBounce = input.filter { it.isDigit() || (it == '.') } },
                        label = { Text("Actual Bounce") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
                    )
                    OutlinedTextField(
                        value = manualActLoaded,
                        onValueChange = { input -> manualActLoaded = input.filter { it.isDigit() || (it == '.') } },
                        label = { Text("Actual Loaded") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done)
                    )
                }
            }
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = OcrDateParser.datePickerMillisForTimestamp(
                    selectedDateMillis,
                    ZoneId.systemDefault()
                )
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val picked = datePickerState.selectedDateMillis
                        if (picked != null) {
                            selectedDateMillis = OcrDateParser.timestampForDatePickerMillis(
                                picked,
                                ZoneId.systemDefault()
                            )
                        }
                        showDatePicker = false
                    }) { Text("OK") }
                }
            ) { DatePicker(state = datePickerState) }
        }

        if (showDeliveryDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = OcrDateParser.datePickerMillisForTimestamp(
                    selectedDeliveryDateMillis,
                    ZoneId.systemDefault()
                )
            )
            DatePickerDialog(
                onDismissRequest = { showDeliveryDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { picked ->
                            selectedDeliveryDateMillis = OcrDateParser.timestampForDatePickerMillis(
                                picked,
                                ZoneId.systemDefault()
                            )
                        }
                        showDeliveryDatePicker = false
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
        OutlinedTextField(
            value = extraStopCount,
            onValueChange = { input ->
                if (input.all { it.isDigit() }) extraStopCount = input
            },
            label = { Text("Stop Offs (payable extra stops)") },
            supportingText = { Text("Enter the Stop Offs count shown on the load paperwork.") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            )
        )
        if (!isManualEntry && extraStopCount.toIntOrNull() != stopDrafts.size) {
            Text(
                "The payable Stop Off count must match the destinations entered below for an active load.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        Text(
            "Route stop-offs (in travel order)",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            "Add each intermediate destination before the final consignee. Leg miles are from the previous destination. OCR suggests stops only when the screenshot labels them as “Stop Off 1 Name/Address/Miles”.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
        stopDrafts.forEachIndexed { index, stop ->
            val alreadyVisited =
                editingLoad != null && index < editingLoad.currentStopIndex
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Stop Off ${index + 1}", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = stop.name,
                    onValueChange = { stopDrafts[index] = stopDrafts[index].copy(name = it) },
                    label = { Text("Stop name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    readOnly = alreadyVisited
                )
                OutlinedTextField(
                    value = stop.address,
                    onValueChange = {
                        stopDrafts[index] = stopDrafts[index].copy(
                            address = it,
                            latitude = null,
                            longitude = null,
                            lookupError = null
                        )
                    },
                    label = { Text("Stop address") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    readOnly = alreadyVisited
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stop.dispatchedLegMiles,
                        onValueChange = { value ->
                            val filtered = value.filter { it.isDigit() || it == '.' }
                            if (filtered.count { it == '.' } <= 1) {
                                stopDrafts[index] =
                                    stopDrafts[index].copy(dispatchedLegMiles = filtered)
                            }
                        },
                        label = { Text("Dispatched leg miles") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        readOnly = alreadyVisited,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        )
                    )
                    TextButton(
                        onClick = { verifyStopAddress(index) },
                        enabled = !alreadyVisited && !stop.isResolving && stop.address.isNotBlank()
                    ) {
                        if (stop.isResolving) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        } else {
                            Text(if (stop.latitude != null) "Verified" else "Verify")
                        }
                    }
                }
                if (stop.dispatchedLegMiles.toDoubleOrNull()?.let { it > 0.0 } != true) {
                    Text(
                        "Enter positive dispatched miles for this leg.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (stop.latitude != null && stop.longitude != null) {
                    Text(
                        "Coordinates verified; GPS arrival tracking is available.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                stop.lookupError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                TextButton(
                    onClick = {
                        stopDrafts.removeAt(index)
                        extraStopCount = stopDrafts.size.toString()
                    },
                    enabled = !alreadyVisited && stopDrafts.none { it.isResolving }
                ) {
                    Text("Remove Stop Off")
                }
                HorizontalDivider()
            }
        }
        TextButton(
            onClick = {
                stopDrafts.add(
                    LoadStopDraft(
                        name = "",
                        address = "",
                        latitude = null,
                        longitude = null,
                        dispatchedLegMiles = ""
                    )
                )
                extraStopCount = stopDrafts.size.toString()
            },
            enabled = stopDrafts.size < 20 && stopDrafts.none { it.isResolving }
        ) {
            Text("Add Stop Off")
        }
        val plannedStopMiles =
            stopDrafts.sumOf { it.dispatchedLegMiles.toDoubleOrNull() ?: 0.0 }
        if (plannedStopMiles > (dLoaded.toDoubleOrNull() ?: 0.0)) {
            Text(
                "Stop-leg miles cannot exceed the load’s total dispatched loaded miles.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
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
            if (isPreTarped && selectedTarp == "None") {
                Text(
                    "Screenshot indicates pre-tarped (P). Select the 4' or 8' drop size to record tarp pay correctly.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
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
                (extraStopCount.isBlank() || extraStopCount.toIntOrNull()?.let { it in 0..20 } == true) &&
                (isManualEntry || extraStopCount.toIntOrNull() == stopDrafts.size) &&
                stopDrafts.all { it.toLoadStopOrNull() != null } &&
                stopDrafts.sumOf { it.dispatchedLegMiles.toDoubleOrNull() ?: 0.0 } <=
                        (dLoaded.toDoubleOrNull() ?: 0.0) &&
                (editingLoad == null ||
                        editingLoad.currentStopIndex <= stopDrafts.size) &&
                sLat != null &&
                cLat != null &&
                (!isPreTarped || selectedTarp != "None")

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
                        percentageRate = ratePct.toDoubleOrNull() ?: 31.0,
                        loadPay = loadPay.toDoubleOrNull() ?: 0.0,
                        tarpType = tChar,
                        isPreTarped = isPreTarped,
                        isGoingHome = isGoingHome,
                        isTrainingWeek = isTrainingWeek,
                        trainerPayRate = resolvedTrainerRate,
                        extraStopCount = extraStopCount.toIntOrNull() ?: 0,
                        stopOffsData = LoadStopCodec.encode(
                            stopDrafts.mapNotNull { it.toLoadStopOrNull() }
                        ),
                        currentStopIndex = editingLoad?.currentStopIndex ?: 0,
                        lastStopLoadedMiles = editingLoad?.lastStopLoadedMiles ?: 0.0,
                        pickupTimestamp = selectedDateMillis,
                        tripState = "COMPLETED",
                        tripNotes = tripNotes.ifBlank { null },
                        deliveryTimestamp = selectedDeliveryDateMillis,
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
                    val actBounce = manualActBounce.toDoubleOrNull() ?: editingLoad?.bounceMilesEnd ?: 0.0
                    val actLoaded = manualActLoaded.toDoubleOrNull() ?: editingLoad?.loadedMilesEnd ?: 0.0

                    val draftData = CurrentLoad(
                        proNumber = proNum.trim(),
                        dispatchedBounceMiles = dispB,
                        dispatchedLoadedMiles = dispL,
                        bounceMilesStart = editingLoad?.bounceMilesStart ?: 0.0,
                        bounceMilesEnd = actBounce,
                        loadedMilesStart = editingLoad?.loadedMilesStart ?: 0.0,
                        loadedMilesEnd = actLoaded,
                        percentageRate = ratePct.toDoubleOrNull() ?: 31.0,
                        loadPay = loadPay.toDoubleOrNull() ?: 0.0,
                        tarpType = tChar,
                        isPreTarped = isPreTarped,
                        isGoingHome = isGoingHome,
                        isTrainingWeek = isTrainingWeek,
                        trainerPayRate = resolvedTrainerRate,
                        extraStopCount = extraStopCount.toIntOrNull() ?: 0,
                        stopOffsData = LoadStopCodec.encode(
                            stopDrafts.mapNotNull { it.toLoadStopOrNull() }
                        ),
                        currentStopIndex = editingLoad?.currentStopIndex ?: 0,
                        lastStopLoadedMiles = editingLoad?.lastStopLoadedMiles ?: 0.0,
                        pickupTimestamp = editingLoad?.pickupTimestamp ?: System.currentTimeMillis(),
                        tripState = editingLoad?.tripState ?: "ACTIVE_BOUNCE",
                        tripNotes = tripNotes.ifBlank { null },
                        deliveryTimestamp = editingLoad?.deliveryTimestamp,
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
                        TrackingService.resetTrackingState()
                        TrackingService.activeProNumber = proNum.trim()
                        TrackingService.targetLat = sLat
                        TrackingService.targetLong = sLong
                        TrackingService.targetName = sName
                        TrackingService.isGeofenceActive = true
                        ContextCompat.startForegroundService(ctx, Intent(ctx, TrackingService::class.java))
                    } else {
                        if (actBounce > 0.0) TrackingService.totalBounceMilesTracked.value = actBounce
                        if (actLoaded > 0.0) TrackingService.totalLoadedMilesTracked.value = actLoaded
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
                    val data = pendingDuplicateLoad!!
                    if (!data.tripState.startsWith("COMPLETED")) {
                        TrackingService.resetTrackingState()
                        TrackingService.activeProNumber = data.proNumber
                        TrackingService.targetLat = data.shipperLat
                        TrackingService.targetLong = data.shipperLong
                        TrackingService.targetName = data.shipperName
                        TrackingService.isGeofenceActive = true
                        ContextCompat.startForegroundService(ctx, Intent(ctx, TrackingService::class.java))
                    }
                    onSaveClick(data)
                    showDuplicateWarningDialog = false
                    pendingDuplicateLoad = null
                }) { Text("Yes, Update") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDuplicateWarningDialog = false
                    pendingDuplicateLoad = null
                }) { Text("Cancel") }
            }
        )
    }
}
