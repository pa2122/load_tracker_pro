package com.example.tmcloadtracker

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacilitySearchScreen(
    viewModel: LoadViewModel,
    onBack: () -> Unit
) {
    val facilities by viewModel.allFacilities.collectAsState(initial = emptyList())
    val selectedName by viewModel.selectedFacility.collectAsState()
    val notes by viewModel.facilityNotes.collectAsState(initial = emptyList())
    
    var searchQuery by remember { mutableStateOf("") }
    val filteredFacilities = facilities.filter { it.contains(searchQuery, ignoreCase = true) }

    // Intercept back button when a facility is selected to return to facility list
    BackHandler(enabled = selectedName.isNotBlank()) {
        viewModel.selectFacility("")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedName.ifBlank { "Facility Insights" }) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedName.isNotBlank()) {
                            viewModel.selectFacility("")
                        } else {
                            onBack()
                        }
                    }) {
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (selectedName.isBlank()) {
                Text("Review historical notes for shippers and receivers.", style = MaterialTheme.typography.bodyMedium)

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
                            if (facilities.isEmpty()) "No saved facilities found in load logs." else "No matching facilities found.",
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredFacilities) { fullName ->
                            val formattedTitle = remember(fullName) { formatFacilityTitle(fullName) }
                            val addressDetails = remember(fullName) {
                                val lines = fullName.lines().filter { it.isNotBlank() }
                                if (lines.size > 1) lines.drop(1).joinToString(", ") else ""
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { 
                                    viewModel.selectFacility(fullName)
                                    searchQuery = ""
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        formattedTitle,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Historical Notes",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = { viewModel.selectFacility("") }) {
                        Text("← All Facilities")
                    }
                }

                if (notes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No notes recorded for $selectedName.", color = MaterialTheme.colorScheme.secondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(notes) { note ->
                            val date = remember(note.pickupTimestamp) {
                                Instant.ofEpochMilli(note.pickupTimestamp)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                                    .format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))
                            }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("PRO #${note.proNumber}", style = MaterialTheme.typography.titleSmall)
                                        Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(note.tripNotes, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatFacilityTitle(fullName: String): String {
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
