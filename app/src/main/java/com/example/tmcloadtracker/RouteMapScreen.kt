package com.example.tmcloadtracker

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteMapScreen(
    load: CurrentLoad?,
    viewModel: LoadViewModel,
    homeLocation: LatLng?,
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    val db = remember { AppDatabase.getDatabase(ctx) }

    var selectedMapType by remember { mutableStateOf(MapType.NORMAL) }
    var selectedTimeFilter by remember { mutableStateOf("All Time") }
    var selectedFacilityLoad by remember { mutableStateOf<CurrentLoad?>(load) }
    var isShipperSelected by remember { mutableStateOf(true) }

    val allLoads by db.loadDao().getAllLoads().collectAsState(initial = emptyList())
    val fuelEntries by db.loadDao().getAllFuelEntries().collectAsState(initial = emptyList())

    val breadcrumbsFlow = remember(load?.proNumber) {
        if (load != null) {
            viewModel.getBreadcrumbs(load.proNumber)
        } else {
            viewModel.allBreadcrumbs
        }
    }
    val breadcrumbs by breadcrumbsFlow.collectAsState(initial = emptyList())

    // Group breadcrumbs by PRO number into separate route paths
    val pathsWithAge = remember(breadcrumbs, selectedTimeFilter) {
        val grouped = breadcrumbs.groupBy { it.proNumber }
        val sortedKeys = grouped.keys.toList()
        grouped.entries.mapIndexed { index, entry ->
            val points = entry.value.map { LatLng(it.latitude, it.longitude) }
            val recencyRatio = if (sortedKeys.size > 1) index.toFloat() / (sortedKeys.size - 1) else 1.0f
            
            // Recency Color Gradient: Recent = Cyan (#00E5FF), Older = Deep Blue/Purple
            val routeColor = when {
                recencyRatio >= 0.7f -> Color(0xFF00E5FF) // Newest: Bright Cyan
                recencyRatio >= 0.4f -> Color(0xFF3D5AFE) // Mid: Royal Blue
                else -> Color(0xFF651FFF)                 // Older: Deep Purple
            }
            Pair(points, routeColor)
        }
    }

    val cameraPositionState = rememberCameraPositionState(key = load?.proNumber) {
        val center = if (load != null) {
            val lat = load.shipperLat ?: homeLocation?.latitude ?: 39.8283
            val lng = load.shipperLong ?: homeLocation?.longitude ?: -98.5795
            LatLng(lat, lng)
        } else {
            LatLng(39.8283, -98.5795)
        }
        position = CameraPosition.fromLatLngZoom(center, if (load != null) 7f else 4f)
    }

    val displayLoads = remember(load, allLoads) {
        if (load != null) listOf(load) else allLoads
    }

    val hasMapContent = (pathsWithAge.isNotEmpty()) || (displayLoads.isNotEmpty()) || (homeLocation != null) || (fuelEntries.isNotEmpty())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (load != null) "Route: PRO #${load.proNumber}" else "Historical Route Heatmap") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (!hasMapContent) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No GPS data or route history recorded yet.", color = MaterialTheme.colorScheme.secondary)
                }
            } else {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(mapType = selectedMapType)
                ) {
                    // Render Recency Gradient Route Lines
                    pathsWithAge.forEach { (path, color) ->
                        if (path.isNotEmpty()) {
                            Polyline(
                                points = path,
                                color = color,
                                width = 12f
                            )
                        }
                    }

                    // Render Shipper and Consignee Pins for all loads
                    displayLoads.forEach { item ->
                        item.shipperLat?.let { lat ->
                            item.shipperLong?.let { lng ->
                                Marker(
                                    state = MarkerState(position = LatLng(lat, lng)),
                                    title = "Shipper: ${item.shipperName ?: "Start"}",
                                    snippet = "Tap for gate & contact info",
                                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
                                    onClick = {
                                        selectedFacilityLoad = item
                                        isShipperSelected = true
                                        true
                                    }
                                )
                            }
                        }

                        item.consigneeLat?.let { lat ->
                            item.consigneeLong?.let { lng ->
                                Marker(
                                    state = MarkerState(position = LatLng(lat, lng)),
                                    title = "Consignee: ${item.consigneeName ?: "End"}",
                                    snippet = "Tap for gate & contact info",
                                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                                    onClick = {
                                        selectedFacilityLoad = item
                                        isShipperSelected = false
                                        true
                                    }
                                )
                            }
                        }
                    }

                    // Render Fuel Stop Pins from fuel_entries table
                    fuelEntries.forEach { entry ->
                        Marker(
                            state = MarkerState(position = LatLng(32.3021, -96.1116)),
                            title = "⛽ ${entry.stationName} (${entry.state})",
                            snippet = "${entry.gallons} gal @ $${entry.pricePerGallon}/gal",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                        )
                    }

                    homeLocation?.let {
                        Marker(
                            state = MarkerState(position = it),
                            title = "Home Base",
                            snippet = "Personal Safe Zone",
                            alpha = 0.85f
                        )
                    }
                }

                // 1. Floating Map Controls (Style + Recency Legend Bar)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilterChip(
                                selected = selectedMapType == MapType.NORMAL,
                                onClick = { selectedMapType = MapType.NORMAL },
                                label = { Text("Map") }
                            )
                            FilterChip(
                                selected = selectedMapType == MapType.SATELLITE,
                                onClick = { selectedMapType = MapType.SATELLITE },
                                label = { Text("Satellite") }
                            )
                            FilterChip(
                                selected = selectedMapType == MapType.HYBRID,
                                onClick = { selectedMapType = MapType.HYBRID },
                                label = { Text("Hybrid") }
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Recency:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("🔵 Newest", style = MaterialTheme.typography.labelSmall, color = Color(0xFF00E5FF))
                            Text("🟣 Older", style = MaterialTheme.typography.labelSmall, color = Color(0xFF651FFF))
                        }
                    }
                }

                // 2. Interactive Facility Insights Bottom Sheet Card
                val activeFacLoad = selectedFacilityLoad
                if (activeFacLoad != null) {
                    val isShipper = isShipperSelected
                    val facName = if (isShipper) (activeFacLoad.shipperName ?: "Shipper Facility") else (activeFacLoad.consigneeName ?: "Consignee Facility")
                    val facLat = if (isShipper) activeFacLoad.shipperLat else activeFacLoad.consigneeLat
                    val facLng = if (isShipper) activeFacLoad.shipperLong else activeFacLoad.consigneeLong
                    val notesText = activeFacLoad.tripNotes ?: "No specific gate notes recorded."

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Place,
                                        contentDescription = null,
                                        tint = if (isShipper) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        if (isShipper) "Shipper Facility" else "Consignee Facility",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isShipper) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                }
                                IconButton(onClick = { selectedFacilityLoad = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close")
                                }
                            }

                            Text("PRO #${activeFacLoad.proNumber} — $facName", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                            if (notesText.isNotBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "📝 Gate Notes: $notesText",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val keywordRegex = Regex("""(?:call|contact|poc|phone|tel)\b.{0,20}?\(?\b(\d{3})\)?[-.\s]?(\d{3})[-.\s]?(\d{4})\b""", RegexOption.IGNORE_CASE)
                                        val match = keywordRegex.find(notesText)
                                        val phoneNum = if (match != null) "${match.groupValues[1]}${match.groupValues[2]}${match.groupValues[3]}" else ""

                                        if (phoneNum.isNotBlank()) {
                                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNum"))
                                            ctx.startActivity(dialIntent)
                                        } else {
                                            Toast.makeText(ctx, "No phone number found in notes for $facName", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Call Office")
                                }

                                Button(
                                    onClick = {
                                        if (facLat != null && facLng != null) {
                                            val gmmIntentUri = Uri.parse("geo:$facLat,$facLng?q=$facLat,$facLng(${Uri.encode(facName)})")
                                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                            ctx.startActivity(mapIntent)
                                        } else {
                                            Toast.makeText(ctx, "No GPS coordinates available for $facName", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Navigate")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
