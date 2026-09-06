package com.example.tmcloadtracker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
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
    // Correctly derive flow based on whether load is selected or global heatmap is active
    val breadcrumbsFlow = remember(load?.proNumber) {
        if (load != null) {
            viewModel.getBreadcrumbs(load.proNumber)
        } else {
            viewModel.allBreadcrumbs
        }
    }
    val breadcrumbs by breadcrumbsFlow.collectAsState(initial = emptyList())
    
    val paths = remember(breadcrumbs) {
        breadcrumbs.groupBy { it.proNumber }.values.map { tripPoints ->
            tripPoints.map { LatLng(it.latitude, it.longitude) }
        }
    }

    // Re-initialize camera position whenever target load changes
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

    val hasMapContent = (paths.isNotEmpty()) || (load != null) || (homeLocation != null)

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
                    Text("No GPS data recorded yet.", color = MaterialTheme.colorScheme.secondary)
                }
            } else {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState
                ) {
                    paths.forEach { path: List<LatLng> ->
                        if (path.isNotEmpty()) {
                            Polyline(
                                points = path,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                width = 8f,
                            )
                        }
                    }

                    if (load != null) {
                        load.shipperLat?.let { lat ->
                            load.shipperLong?.let { lng ->
                                Marker(
                                    state = MarkerState(position = LatLng(lat, lng)),
                                    title = "Shipper: ${load.shipperName ?: "Start"}",
                                    snippet = "Pickup Point"
                                )
                            }
                        }

                        load.consigneeLat?.let { lat ->
                            load.consigneeLong?.let { lng ->
                                Marker(
                                    state = MarkerState(position = LatLng(lat, lng)),
                                    title = "Consignee: ${load.consigneeName ?: "End"}",
                                    snippet = "Delivery Point"
                                )
                            }
                        }
                    }

                    homeLocation?.let {
                        Marker(
                            state = MarkerState(position = it),
                            title = "Home Base",
                            snippet = "Personal Safe Zone",
                            alpha = 0.8f
                        )
                    }
                }
            }
        }
    }
}
