package com.example.tmcloadtracker

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Facility Insights") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            Text("Review historical notes for shippers and receivers.", style = MaterialTheme.typography.bodyMedium)

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Facility Name") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            )

            if (selectedName.isBlank() || searchQuery.isNotEmpty()) {
                Text("Select a Facility:", style = MaterialTheme.typography.titleSmall)
                LazyColumn(
                    modifier = Modifier.weight(0.4f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredFacilities) { name ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { 
                                viewModel.selectFacility(name)
                                searchQuery = "" // Clear search to focus on results
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (name == selectedName) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(name, modifier = Modifier.padding(12.dp))
                        }
                    }
                }
            }

            if (selectedName.isNotBlank()) {
                HorizontalDivider()
                Text("Historical Notes: $selectedName", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                
                if (notes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No notes found for this facility.", color = MaterialTheme.colorScheme.secondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
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
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("PRO #${note.proNumber}", style = MaterialTheme.typography.labelLarge)
                                        Text(date, style = MaterialTheme.typography.labelSmall)
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
