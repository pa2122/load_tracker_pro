package com.loadtracker.pro

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val EXTERNAL_STORAGE_DOCUMENTS_AUTHORITY = "com.android.externalstorage.documents"

internal data class BatchScreenshot(
    val fileName: String,
    val rawText: String,
    val proNumber: String,
    val pickupDate: String,
    val deliveryDate: String,
    val grossPay: String,
    val bounceMiles: String,
    val loadedMiles: String,
    val percentageRate: String,
    val tarpType: String,
    val isPreTarped: Boolean,
    val isGoingHome: Boolean,
    val shipperName: String,
    val consigneeName: String,
    val error: String? = null,
    val extraStopCount: String = "0"
)

internal fun BatchScreenshot.hasDuplicateProNumber(
    existingProNumbers: Set<String>,
    batchProNumberCounts: Map<String, Int>
): Boolean {
    val normalizedProNumber = proNumber.trim()
    return normalizedProNumber.isNotBlank() &&
            (normalizedProNumber in existingProNumbers || batchProNumberCounts[normalizedProNumber] != 1)
}

internal fun BatchScreenshot.isReadyToImport(
    existingProNumbers: Set<String>,
    batchProNumberCounts: Map<String, Int>
): Boolean {
    fun String.isNonNegativeFiniteNumber() =
        toDoubleOrNull()?.let { it.isFinite() && it >= 0.0 } == true

    return proNumber.isNotBlank() &&
            pickupDate.toLocalDateOrNull() != null &&
            (deliveryDate.isBlank() || deliveryDate.toLocalDateOrNull() != null) &&
            grossPay.isNonNegativeFiniteNumber() &&
            bounceMiles.isNonNegativeFiniteNumber() &&
            loadedMiles.isNonNegativeFiniteNumber() &&
            percentageRate.isNonNegativeFiniteNumber() &&
            (extraStopCount.isBlank() || extraStopCount.toIntOrNull()?.let { it >= 0 } == true) &&
            (!isPreTarped || tarpType != "None") &&
            !hasDuplicateProNumber(existingProNumbers, batchProNumberCounts)
}

private data class ImageDocument(val uri: Uri, val name: String, val mimeType: String)

@Composable
fun ScreenshotBatchImportScreen(
    existingProNumbers: Set<String>,
    defaultPercentage: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drafts = remember { mutableStateListOf<BatchScreenshot>() }
    var isScanning by remember { mutableStateOf(false) }
    var scanStatus by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }
    var showImportConfirmation by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var expandedDrafts by remember { mutableStateOf(emptySet<Int>()) }
    var expandedOcrDrafts by remember { mutableStateOf(emptySet<Int>()) }
    val initialFolderUri = remember {
        DocumentsContract.buildTreeDocumentUri(
            EXTERNAL_STORAGE_DOCUMENTS_AUTHORITY,
            "primary:Documents/LTP_Batch_Upload"
        )
    }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            isScanning = true
            errorText = ""
            drafts.clear()
            expandedDrafts = emptySet()
            expandedOcrDrafts = emptySet()
            scope.launch {
                try {
                    val results = withContext(Dispatchers.IO) {
                        scanScreenshotFolder(context, treeUri, defaultPercentage)
                    }
                    drafts.addAll(results)
                    if (results.isEmpty()) {
                        errorText = "No image files were found in the selected folder."
                    } else {
                        val counts = results.groupingBy { it.proNumber.trim() }.eachCount()
                        val existing = existingProNumbers.mapTo(mutableSetOf()) { it.trim() }
                        val problemCount = results.count { !it.isReadyToImport(existing, counts) }
                        scanStatus = context.getString(
                            R.string.batch_import_scan_status,
                            results.size,
                            problemCount
                        )
                    }
                } catch (e: Exception) {
                    errorText = "Could not read the selected folder: ${e.localizedMessage ?: e.javaClass.simpleName}"
                } finally {
                    isScanning = false
                }
            }
        }
    }

    val existingProNumbersTrimmed = remember(existingProNumbers) {
        existingProNumbers.mapTo(mutableSetOf()) { it.trim() }
    }
    val proNumberCounts = drafts.groupingBy { it.proNumber.trim() }.eachCount()
    val importable = drafts.filter {
        it.isReadyToImport(existingProNumbersTrimmed, proNumberCounts)
    }
    val duplicateCount = drafts.count { it.hasDuplicateProNumber(existingProNumbersTrimmed, proNumberCounts) }
    val needsReviewCount = drafts.size - importable.size - duplicateCount

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.batch_import_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.batch_import_description),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            stringResource(R.string.batch_import_actual_mileage_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            stringResource(R.string.batch_import_stopoff_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )

        Button(
            onClick = { folderPicker.launch(initialFolderUri) },
            enabled = !isScanning && !isImporting,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.batch_import_folder_link))
        }

        if (isScanning || isImporting) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator()
                Text(if (isScanning) "Reading screenshots…" else "Saving historical loads…")
            }
        }

        if (scanStatus.isNotBlank()) Text(scanStatus, color = MaterialTheme.colorScheme.primary)
        if (errorText.isNotBlank()) Text(errorText, color = MaterialTheme.colorScheme.error)

        drafts.forEachIndexed { index, draft ->
            val duplicate = draft.hasDuplicateProNumber(existingProNumbersTrimmed, proNumberCounts)
            val ready = draft.isReadyToImport(existingProNumbersTrimmed, proNumberCounts)
            val expanded = index in expandedDrafts
            val facilitySummary = listOf(draft.shipperName, draft.consigneeName)
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .joinToString(" → ")
                .ifBlank { stringResource(R.string.batch_import_facilities_missing) }
            val pickupDate = draft.pickupDate.ifBlank {
                stringResource(R.string.batch_import_pickup_missing)
            }
            val payAmount = draft.grossPay.toDoubleOrNull()?.let {
                String.format(Locale.US, "%.2f", it)
            } ?: stringResource(R.string.batch_import_pay_missing)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expandedDrafts = if (expanded) expandedDrafts - index else expandedDrafts + index
                        }
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            draft.proNumber.trim().takeIf { it.isNotBlank() }?.let {
                                stringResource(R.string.batch_import_pro_number, it)
                            }
                                ?: draft.fileName,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            stringResource(
                                when {
                                    duplicate -> R.string.batch_import_status_duplicate
                                    ready -> R.string.batch_import_status_ready
                                    else -> R.string.batch_import_status_needs_review
                                }
                            ),
                            color = when {
                                duplicate || !ready -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.primary
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            stringResource(
                                if (expanded) R.string.batch_import_hide_details
                                else R.string.batch_import_review_details
                            ),
                            modifier = Modifier.padding(start = 8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Text("$pickupDate · $facilitySummary", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(
                            R.string.batch_import_miles_summary,
                            payAmount,
                            draft.bounceMiles.ifBlank { "—" },
                            draft.loadedMiles.ifBlank { "—" }
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (expanded) {
                        Text(draft.fileName, style = MaterialTheme.typography.bodySmall)
                        draft.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        if (duplicate) {
                            Text(
                                stringResource(R.string.batch_import_duplicate_notice),
                                color = MaterialTheme.colorScheme.error
                            )
                        } else if (!ready) {
                            Text(
                                stringResource(R.string.batch_import_review_notice),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        BatchField("PRO number", draft.proNumber) {
                            drafts[index] = draft.copy(proNumber = it)
                        }
                        BatchField("Pickup date (MM/dd/yyyy)", draft.pickupDate) {
                            drafts[index] = draft.copy(pickupDate = it)
                        }
                        BatchField("Delivery date (optional)", draft.deliveryDate) {
                            drafts[index] = draft.copy(deliveryDate = it)
                        }
                        BatchField("Shipper / pickup", draft.shipperName) {
                            drafts[index] = draft.copy(shipperName = it)
                        }
                        BatchField("Consignee / delivery", draft.consigneeName) {
                            drafts[index] = draft.copy(consigneeName = it)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BatchField("Gross pay", draft.grossPay, Modifier.weight(1f)) {
                                drafts[index] = draft.copy(grossPay = it)
                            }
                            BatchField("Pay split %", draft.percentageRate, Modifier.weight(1f)) {
                                drafts[index] = draft.copy(percentageRate = it)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BatchField("Bounce miles", draft.bounceMiles, Modifier.weight(1f)) {
                                drafts[index] = draft.copy(bounceMiles = it)
                            }
                            BatchField("Loaded miles", draft.loadedMiles, Modifier.weight(1f)) {
                                drafts[index] = draft.copy(loadedMiles = it)
                            }
                        }
                        BatchField(
                            "Stop Offs (payable extra stops)",
                            draft.extraStopCount,
                            keyboardType = KeyboardType.Number
                        ) {
                            drafts[index] = draft.copy(extraStopCount = it)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("None", "4' Drop", "8' Drop").forEach { option ->
                                TextButton(onClick = { drafts[index] = draft.copy(tarpType = option) }) {
                                    Text(if (draft.tarpType == option) "✓ $option" else option)
                                }
                            }
                        }
                        Row {
                            Checkbox(
                                checked = draft.isPreTarped,
                                onCheckedChange = { drafts[index] = draft.copy(isPreTarped = it) }
                            )
                            Text("Pre-tarped")
                        }
                        Row {
                            Checkbox(
                                checked = draft.isGoingHome,
                                onCheckedChange = { drafts[index] = draft.copy(isGoingHome = it) }
                            )
                            Text("Going home")
                        }
                        if (draft.rawText.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    expandedOcrDrafts = if (index in expandedOcrDrafts) {
                                        expandedOcrDrafts - index
                                    } else {
                                        expandedOcrDrafts + index
                                    }
                                }
                            ) {
                                Text(if (index in expandedOcrDrafts) "Hide OCR text" else "Show OCR text")
                            }
                        }
                    }
                    if (expanded && index in expandedOcrDrafts && draft.rawText.isNotBlank()) {
                        Text(
                            draft.rawText,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 12,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        if (drafts.isNotEmpty()) {
            Text(
                stringResource(
                    R.string.batch_import_review_counts,
                    importable.size,
                    duplicateCount,
                    needsReviewCount
                ),
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = { showImportConfirmation = true },
                enabled = !isImporting && importable.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.batch_import_import_ready, importable.size))
            }
        }

        HorizontalDivider()
        TextButton(onClick = onBack, enabled = !isImporting) { Text("Back") }
    }

    if (showImportConfirmation) {
        AlertDialog(
            onDismissRequest = { showImportConfirmation = false },
            title = { Text(stringResource(R.string.batch_import_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.batch_import_confirm_message,
                        importable.size,
                        duplicateCount,
                        needsReviewCount
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showImportConfirmation = false
                        isImporting = true
                        errorText = ""
                        scope.launch {
                            try {
                                val loads = importable.map { it.toHistoricalLoad() }
                                val backupSaved = withContext(Dispatchers.IO) {
                                    val snapshotSaved =
                                        DatabaseBackupManager.createDailyBackupBeforeMutation(context)
                                    AppDatabase.getDatabase(context).loadDao()
                                        .insertHistoricalBatchIfNoConflicts(loads)
                                    snapshotSaved
                                }
                                scanStatus = if (backupSaved) {
                                    context.getString(
                                        R.string.batch_import_success,
                                        loads.size,
                                        duplicateCount,
                                        needsReviewCount
                                    )
                                } else {
                                    context.getString(
                                        R.string.batch_import_snapshot_warning,
                                        loads.size,
                                        duplicateCount,
                                        needsReviewCount
                                    )
                                }
                            } catch (e: Exception) {
                                errorText = "Import failed: ${e.localizedMessage ?: e.javaClass.simpleName}"
                            } finally {
                                isImporting = false
                            }
                        }
                    }
                ) { Text("Confirm Import") }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirmation = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun BatchField(
    label: String,
    value: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
    )
}

private suspend fun scanScreenshotFolder(
    context: Context,
    treeUri: Uri,
    defaultPercentage: String
): List<BatchScreenshot> {
    val files = listImageDocuments(context, treeUri)
    if (files.isEmpty()) return emptyList()

    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    try {
        return files.map { file ->
            try {
                val image = InputImage.fromFilePath(context, file.uri)
                val text = OcrHelper.reconstructSpatialText(recognizer.process(image).await())
                if (text.isBlank()) {
                    emptyDraft(file.name, defaultPercentage, "No text was detected.")
                } else {
                    ScreenshotBatchParser.parse(file.name, text, defaultPercentage)
                }
            } catch (e: Exception) {
                Log.e("ScreenshotBatchImport", "OCR failed for ${file.name}", e)
                emptyDraft(
                    file.name,
                    defaultPercentage,
                    "Could not scan this image: ${e.localizedMessage ?: e.javaClass.simpleName}"
                )
            }
        }
    } finally {
        recognizer.close()
    }
}

private fun listImageDocuments(context: Context, treeUri: Uri): List<ImageDocument> {
    val resolver = context.contentResolver
    val results = mutableListOf<ImageDocument>()
    val visited = mutableSetOf<String>()
    val rootId = DocumentsContract.getTreeDocumentId(treeUri)

    fun visit(documentId: String) {
        if (!visited.add(documentId)) return
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
        resolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ),
            null,
            null,
            null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            while (cursor.moveToNext()) {
                val childId = cursor.getString(idIndex)
                val name = cursor.getString(nameIndex).orEmpty()
                val mimeType = cursor.getString(mimeIndex).orEmpty()
                if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    visit(childId)
                } else if (mimeType.startsWith("image/") ||
                    name.substringAfterLast('.', "").lowercase(Locale.US) in
                    setOf("jpg", "jpeg", "png", "webp", "heic")
                ) {
                    results += ImageDocument(
                        DocumentsContract.buildDocumentUriUsingTree(treeUri, childId),
                        name.ifBlank { "Screenshot ${results.size + 1}" },
                        mimeType
                    )
                }
            }
        }
    }

    visit(rootId)
    return results
}

internal object ScreenshotBatchParser {
    fun parse(fileName: String, rawText: String, defaultPercentage: String): BatchScreenshot {
        val pro = Regex(
            """\b(?:PRO|Order|Load|Trip)\s*(?:Number|No|Num)?\s*#?\s*:?\s*(\d{4,12})""",
            RegexOption.IGNORE_CASE
        ).find(rawText)?.groupValues?.get(1)
            ?: Regex("""\b\d{6,10}\b""").find(rawText)?.value.orEmpty()

        val pay = Regex(
            """(?:Gross\s*Pay|Gross|Pay|Linehaul|Total\s*Pay|Amount)\D*?\$?\s*(\d{1,3}(?:,\d{3})+|\d+)(?:\.(\d{1,2}))?""",
            RegexOption.IGNORE_CASE
        ).find(rawText)?.let { match ->
            match.groupValues[1].replace(",", "") +
                    match.groupValues[2].takeIf { it.isNotBlank() }?.let { ".$it" }.orEmpty()
        }.orEmpty()

        fun extractMiles(pattern: Regex): String = pattern.find(rawText)
            ?.groupValues?.get(1)?.replace(",", "").orEmpty()

        val bounce = extractMiles(
            Regex(
                """(?:bounce\s*miles|bounce\s*mi|deadhead\s*miles|deadhead\s*mi|dh\s*miles)\s*[:=\-\s]*(\d{1,4}(?:,\d{3})*(?:\.\d+)?)""",
                RegexOption.IGNORE_CASE
            )
        )
        val loaded = extractMiles(
            Regex(
                """(?:loaded\s*miles|loaded\s*mi|load\s*miles|trip\s*miles|distance)\s*[:=\-\s]*(\d{1,4}(?:,\d{3})*(?:\.\d+)?)""",
                RegexOption.IGNORE_CASE
            )
        )
        val pickupDate = rawText.lines().firstNotNullOfOrNull { line ->
            if (Regex("""\b(pick[\s-]?up|origin|shipper)\b""", RegexOption.IGNORE_CASE)
                    .containsMatchIn(line)
            ) {
                BATCH_DATE.find(line)?.value
            } else null
        }.orEmpty()
        val deliveryDate = OcrDateParser.extractDeliveryDateMillis(rawText)?.let { timestamp ->
            java.time.Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .format(DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.US))
        }.orEmpty()
        var currentFacility: String? = null
        val shipperName = StringBuilder()
        val consigneeName = StringBuilder()
        rawText.lines().forEach { line ->
            val trimmed = line.trim()
            val lower = trimmed.lowercase(Locale.US)
            val shipperHeader = Regex(
                """^(shipper|pickup|origin)(?:\s+(?:name|location|address))?\s*:?\s*(.*)$""",
                RegexOption.IGNORE_CASE
            ).find(trimmed)
            val consigneeHeader = Regex(
                """^(consignee|delivery|destination|dropoff)(?:\s+(?:name|location|address))?\s*:?\s*(.*)$""",
                RegexOption.IGNORE_CASE
            ).find(trimmed)
            when {
                lower.startsWith("pickup date") || lower.startsWith("pickup appt") ||
                        lower.startsWith("pickup appointment") || lower.startsWith("delivery date") ||
                        lower.startsWith("delivery appt") || lower.startsWith("delivery appointment") ||
                        lower.startsWith("going home") || lower.startsWith("home run") ||
                        StopOffParser.isLabelLine(trimmed) ||
                        lower.startsWith("tarp") ->
                    currentFacility = null
                shipperHeader != null -> {
                    currentFacility = "shipper"
                    shipperHeader.groupValues[2].takeIf { it.isNotBlank() }?.let {
                        shipperName.append(it.trim()).append('\n')
                    }
                }
                consigneeHeader != null -> {
                    currentFacility = "consignee"
                    consigneeHeader.groupValues[2].takeIf { it.isNotBlank() }?.let {
                        consigneeName.append(it.trim()).append('\n')
                    }
                }
                lower.startsWith("pro ") || lower.startsWith("gross ") ||
                        lower.startsWith("bounce ") || lower.startsWith("loaded ") ||
                        lower.startsWith("stop type") -> currentFacility = null
                currentFacility == "shipper" && shipperName.lines().size < 4 ->
                    shipperName.append(trimmed).append('\n')
                currentFacility == "consignee" && consigneeName.lines().size < 4 ->
                    consigneeName.append(trimmed).append('\n')
            }
        }
        val goingHome = Regex(
            """(?:Going\s*Home|Home\s*Run|Home)\s*[:=\-\s]*([YN])""",
            RegexOption.IGNORE_CASE
        ).find(rawText)?.groupValues?.get(1)?.equals("Y", ignoreCase = true) == true

        val tarp = OcrTarpParser.parse(rawText)
        val extraStopCount = StopOffParser.parseCount(rawText) ?: 0
        val tarpType = when (tarp?.tarpType) {
            "4' Drop" -> "4' Drop"
            "8' Drop" -> "8' Drop"
            else -> "None"
        }

        return BatchScreenshot(
            fileName = fileName,
            rawText = rawText,
            proNumber = pro,
            pickupDate = pickupDate,
            deliveryDate = deliveryDate,
            grossPay = pay,
            bounceMiles = bounce,
            loadedMiles = loaded,
            percentageRate = defaultPercentage,
            tarpType = tarpType,
            isPreTarped = tarp?.isPreTarped == true,
            isGoingHome = goingHome,
            shipperName = shipperName.toString().trim(),
            consigneeName = consigneeName.toString().trim(),
            extraStopCount = extraStopCount.toString()
        )
    }
}

private val BATCH_DATE = Regex("""\b(?:\d{1,2}[/-]\d{1,2}[/-]\d{2,4}|\d{4}-\d{1,2}-\d{1,2})\b""")

private fun emptyDraft(fileName: String, defaultPercentage: String, error: String) =
    BatchScreenshot(
        fileName = fileName,
        rawText = "",
        proNumber = "",
        pickupDate = "",
        deliveryDate = "",
        grossPay = "",
        bounceMiles = "",
        loadedMiles = "",
        percentageRate = defaultPercentage,
        tarpType = "None",
        isPreTarped = false,
        isGoingHome = false,
        shipperName = "",
        consigneeName = "",
        extraStopCount = "0",
        error = error
    )

private fun String.toLocalDateOrNull(): LocalDate? {
    val trimmed = trim()
    val patterns = listOf("M/d/yyyy", "M-d-yyyy", "M/d/yy", "M-d-yy", "yyyy-M-d")
    return patterns.firstNotNullOfOrNull { pattern ->
        try {
            LocalDate.parse(trimmed, DateTimeFormatter.ofPattern(pattern, Locale.US))
        } catch (_: Exception) {
            null
        }
    }
}

private fun BatchScreenshot.toHistoricalLoad(): CurrentLoad {
    val date = requireNotNull(pickupDate.toLocalDateOrNull())
    val pickupTimestamp = date.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val bounce = requireNotNull(bounceMiles.toDoubleOrNull())
    val loaded = requireNotNull(loadedMiles.toDoubleOrNull())
    val tarpCode = when (tarpType) {
        "4' Drop" -> "S"
        "8' Drop" -> "L"
        else -> "N"
    }
    return CurrentLoad(
        proNumber = proNumber.trim(),
        dispatchedBounceMiles = bounce,
        dispatchedLoadedMiles = loaded,
        bounceMilesStart = 0.0,
        bounceMilesEnd = bounce,
        loadedMilesStart = 0.0,
        loadedMilesEnd = loaded,
        percentageRate = requireNotNull(percentageRate.toDoubleOrNull()),
        loadPay = requireNotNull(grossPay.toDoubleOrNull()),
        tarpType = tarpCode,
        isPreTarped = isPreTarped,
        pickupTimestamp = pickupTimestamp,
        isGoingHome = isGoingHome,
        tripState = "COMPLETED",
        tripNotes = "Imported from screenshot batch: $fileName",
        extraStopCount = extraStopCount.toIntOrNull() ?: 0,
        deliveryTimestamp = deliveryDate.toLocalDateOrNull()?.atTime(12, 0)
            ?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
        shipperName = shipperName.ifBlank { null },
        consigneeName = consigneeName.ifBlank { null }
    )
}
