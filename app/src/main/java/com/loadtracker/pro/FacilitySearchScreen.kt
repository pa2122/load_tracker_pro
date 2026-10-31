package com.loadtracker.pro

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacilitySearchScreen(
    viewModel: LoadViewModel,
    onBack: () -> Unit
) {
    val facilities by viewModel.facilities.collectAsState(initial = emptyList())
    val selectedKey by viewModel.selectedFacility.collectAsState()
    val selected = facilities.firstOrNull { it.facilityKey == selectedKey }
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf(false) }
    val filteredFacilities = facilities.filter { it.displayName.contains(searchQuery, ignoreCase = true) }

    // Intercept back button when a facility is selected to return to facility list
    BackHandler(enabled = selected != null) {
        viewModel.selectFacility("")
    }

    Scaffold(
        topBar = {
            if (selected != null) {
                TopAppBar(
                    title = { Text(selected.displayName) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.selectFacility("") }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (selected == null) {
                Text("Quick reference notes for shippers and receivers.", style = MaterialTheme.typography.bodyMedium)

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Facility Name") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )

                Text("Select a Facility (${filteredFacilities.size}):", style = MaterialTheme.typography.titleSmall)

                if (filteredFacilities.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            if (facilities.isEmpty()) "No facility notes saved yet." else "No matching facilities found.",
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredFacilities, key = { it.facilityKey }) { facility ->
                            val formattedTitle = remember(facility.displayName) { formatFacilityTitle(facility.displayName) }
                            val addressDetails = remember(facility.displayName) {
                                val lines = facility.displayName.lines().filter { it.isNotBlank() }
                                if (lines.size > 1) lines.drop(1).joinToString(", ") else ""
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    viewModel.selectFacility(facility.facilityKey)
                                    searchQuery = ""
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(formattedTitle, style = MaterialTheme.typography.bodyLarge)
                                    if (addressDetails.isNotBlank()) {
                                        Text(
                                            addressDetails,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                FacilityDetail(
                    facility = selected,
                    onEdit = { editing = true },
                    onBack = { viewModel.selectFacility("") }
                )
            }
        }
    }

    if (editing && selected != null) {
        FacilityNotesEditDialog(
            facility = selected,
            onDismiss = { editing = false },
            onSave = { fields ->
                scope.launch {
                    viewModel.saveFacilityNote(
                        selected.copy(
                            instructions = fields.instructions.trim(),
                            directions = fields.directions.trim(),
                            parking = fields.parking.trim(),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
                editing = false
            }
        )
    }
}

@Composable
private fun FacilityDetail(
    facility: FacilityNoteEntity,
    onEdit: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val facilityTitle = remember(facility.displayName) { formatFacilityTitle(facility.displayName) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Facility Notes",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        TextButton(onClick = onBack) {
            Text("← All Facilities")
        }
    }

    Button(
        onClick = {
            val geoUri = Uri.parse("geo:0,0?q=${Uri.encode(facility.displayName)}")
            val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
            val chooser = Intent.createChooser(mapIntent, "Navigate with Trucker Path or Maps")
            try {
                context.startActivity(chooser)
            } catch (_: Exception) {
                Toast.makeText(context, "No navigation app found on device.", Toast.LENGTH_LONG).show()
            }
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("🧭 Launch Navigation ($facilityTitle)")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            val sections = listOf(
                "⚠️ Instructions" to facility.instructions,
                "🗺️ Directions" to facility.directions,
                "🅿️ Parking" to facility.parking
            ).filter { it.second.isNotBlank() }

            if (sections.isEmpty()) {
                Text("No notes yet. Tap Edit to add some.", color = MaterialTheme.colorScheme.secondary)
            }
            sections.forEachIndexed { index, (label, value) ->
                if (index > 0) Spacer(modifier = Modifier.height(12.dp))
                Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            val phone = extractPhoneNumber("${facility.instructions}\n${facility.directions}\n${facility.parking}")
            if (phone != null) {
                val (dialableNumber, displayString) = phone
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$dialableNumber"))
                        try {
                            context.startActivity(dialIntent)
                        } catch (_: Exception) {
                            Toast.makeText(context, "No dialer app found.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📞 Call Shipping Office: $displayString")
                }
            }
        }
    }

    Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
        Text("Edit Notes")
    }
}

@Composable
private fun FacilityNotesEditDialog(
    facility: FacilityNoteEntity,
    onDismiss: () -> Unit,
    onSave: (FacilityNoteFields) -> Unit
) {
    var instructions by remember { mutableStateOf(facility.instructions) }
    var directions by remember { mutableStateOf(facility.directions) }
    var parking by remember { mutableStateOf(facility.parking) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Facility Notes") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Instructions") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                OutlinedTextField(
                    value = directions,
                    onValueChange = { directions = it },
                    label = { Text("Directions") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                OutlinedTextField(
                    value = parking,
                    onValueChange = { parking = it },
                    label = { Text("Parking") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(FacilityNoteFields(instructions, directions, parking)) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

internal fun formatFacilityTitle(fullName: String): String {
    val lines = fullName.lines().filter { it.isNotBlank() }
    if (lines.isEmpty()) return fullName
    val mainName = lines[0].trim()

    val cityStateRegex = Regex("""([A-Za-z\s]+),\s*([A-Z]{2})\b""")
    for (i in 1 until lines.size) {
        val match = cityStateRegex.find(lines[i])
        if (match != null) {
            return "$mainName — ${match.value}"
        }
    }
    return mainName
}

private fun extractPhoneNumber(text: String): Pair<String, String>? {
    val keywordRegex = Regex("""(?:call|contact|poc|phone|tel)\b.{0,20}?\(?\b(\d{3})\)?[-.\s]?(\d{3})[-.\s]?(\d{4})\b""", RegexOption.IGNORE_CASE)
    val keywordMatch = keywordRegex.find(text)

    if (keywordMatch != null && keywordMatch.groupValues.size >= 4) {
        val area = keywordMatch.groupValues[1]
        val mid = keywordMatch.groupValues[2]
        val end = keywordMatch.groupValues[3]
        return Pair("$area$mid$end", "($area) $mid-$end")
    }

    val standardRegex = Regex("""\(?\b(\d{3})\)?[-.\s]+(\d{3})[-.\s]+(\d{4})\b""")
    val standardMatch = standardRegex.find(text)

    if (standardMatch != null && standardMatch.groupValues.size >= 4) {
        val area = standardMatch.groupValues[1]
        val mid = standardMatch.groupValues[2]
        val end = standardMatch.groupValues[3]
        return Pair("$area$mid$end", "($area) $mid-$end")
    }

    return null
}
