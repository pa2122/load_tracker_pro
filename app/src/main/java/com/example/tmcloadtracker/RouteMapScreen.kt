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
import androidx.compose.ui.unit.dp
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
    val breadcrumbs by if (load != null) {
        viewModel.getBreadcrumbs(load.proNumber).collectAsState(initial = emptyList())
    } else {
        viewModel.allBreadcrumbs.collectAsState(initial = emptyList())
    }
    
    val paths = remember(breadcrumbs) {
        breadcrumbs.groupBy { it.proNumber }.values.map { tripPoints ->
            tripPoints.map { LatLng(it.latitude, it.longitude) }
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        val center = if (load != null) {
            val lat = load.shipperLat ?: 39.8283
            val lng = load.shipperLong ?: -98.5795
            LatLng(lat, lng)
        } else {
            LatLng(39.8283, -98.5795)
        }
        position = CameraPosition.fromLatLngZoom(center, if (load != null) 7f else 4f)
    }

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
            if (paths.isEmpty() && (load?.shipperLat == null)) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No GPS data available.", color = MaterialTheme.colorScheme.secondary)
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
