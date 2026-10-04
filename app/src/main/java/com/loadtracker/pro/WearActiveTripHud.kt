package com.loadtracker.pro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import java.util.Locale

@Composable
fun WearActiveTripHud(
    activeLoad: CurrentLoad?,
    onWristAction: (String) -> Unit
) {
    val ctx = LocalContext.current
    var isConnectedToPhone by remember { mutableStateOf(true) }
    var remoteLoadPayload by remember { mutableStateOf<WearableDataSyncManager.WearTripStatePayload?>(null) }

    // Send single request to phone for active load when watch opens
    LaunchedEffect(Unit) {
        try {
            val nodeClient = Wearable.getNodeClient(ctx)
            val nodes = nodeClient.connectedNodes.await()
            val targetNode = nodes.firstOrNull()?.id
            if (targetNode != null) {
                Wearable.getMessageClient(ctx).sendMessage(
                    targetNode,
                    WearableDataSyncManager.PATH_REQUEST_ACTIVE_TRIP_STATE,
                    ByteArray(0)
                ).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Listen continuously to live active_trip_state DataEvents from phone
    DisposableEffect(Unit) {
        val listener = DataClient.OnDataChangedListener { dataEvents ->
            for (event in dataEvents) {
                if (event.dataItem.uri.path == WearableDataSyncManager.PATH_ACTIVE_TRIP_STATE) {
                    val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                    val pro = dataMap.getString("proNumber", "")
                    val state = dataMap.getString("tripState", "")
                    val bounce = dataMap.getDouble("bounceMiles", 0.0)
                    val loaded = dataMap.getDouble("loadedMiles", 0.0)
                    val dockTime = dataMap.getLong("dockArrivalTime", 0L)

                    if (pro.isNotBlank() && state != "COMPLETED") {
                        remoteLoadPayload = WearableDataSyncManager.WearTripStatePayload(
                            proNumber = pro,
                            tripState = state,
                            bounceMiles = bounce,
                            loadedMiles = loaded,
                            dockArrivalTime = dockTime
                        )
                    } else {
                        remoteLoadPayload = null
                    }
                    isConnectedToPhone = true
                }
            }
        }

        Wearable.getDataClient(ctx).addListener(listener)

        onDispose {
            Wearable.getDataClient(ctx).removeListener(listener)
        }
    }

    // Reset remote payload when local DB load completes or clears
    LaunchedEffect(activeLoad?.tripState) {
        if (activeLoad == null || activeLoad.tripState == "COMPLETED") {
            remoteLoadPayload = null
        }
    }

    // Effective active load: prioritize live Wearable DataMap from phone, fallback to local DB load
    val effectivePro = if (activeLoad?.tripState == "COMPLETED") null else (remoteLoadPayload?.proNumber ?: activeLoad?.proNumber)
    val effectiveState = if (activeLoad?.tripState == "COMPLETED") "COMPLETED" else (remoteLoadPayload?.tripState ?: activeLoad?.tripState)
    val effectiveBounce = remoteLoadPayload?.bounceMiles ?: (if (activeLoad != null && activeLoad.bounceMilesEnd > 0) activeLoad.bounceMilesEnd else activeLoad?.dispatchedBounceMiles ?: 0.0)
    val effectiveLoaded = remoteLoadPayload?.loadedMiles ?: (if (activeLoad != null && activeLoad.loadedMilesEnd > 0) activeLoad.loadedMilesEnd else activeLoad?.dispatchedLoadedMiles ?: 0.0)
    val effectiveDockTime = remoteLoadPayload?.dockArrivalTime ?: activeLoad?.dockArrivalTime

    val hasActiveLoad = isConnectedToPhone &&
            !effectivePro.isNullOrBlank() && 
            effectiveState != "COMPLETED" && 
            (activeLoad == null || activeLoad.tripState != "COMPLETED")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)), // Steel Navy Dark
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 28.dp, bottom = 28.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Connection Status Badge at Top
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(if (isConnectedToPhone) Color(0xFF22C55E) else Color(0xFFEF4444), shape = CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isConnectedToPhone) "PHONE CONNECTED" else "DISCONNECTED",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isConnectedToPhone) Color(0xFF86EFAC) else Color(0xFFFCA5A5),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            if (!hasActiveLoad) {
                // NO ACTIVE TRIP SCREEN
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "📍 No Active Trip",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Start a load on your phone to track on wrist.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        fontSize = 10.sp
                    )
                }
            } else {
                // ACTIVE TRIP WRIST HUD SCREEN
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        "PRO #$effectivePro",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFF59E0B), // Gold
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )

                    val statusText = when (effectiveState) {
                        "ACTIVE_BOUNCE" -> "EN ROUTE TO SHIPPER"
                        "ACTIVE_SHIPPER" -> "AT SHIPPER DOCK"
                        "ACTIVE_LOADED" -> "EN ROUTE TO CONSIGNEE"
                        "ACTIVE_CONSIGNEE" -> "AT CONSIGNEE DOCK"
                        else -> "TRIP IN PROGRESS"
                    }

                    Surface(
                        color = Color(0xFF3B82F6), // Cobalt Blue
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            statusText,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Dock Wait Time Calculation (if at Shipper or Consignee Dock)
                val isAtDock = effectiveState == "ACTIVE_SHIPPER" || effectiveState == "ACTIVE_CONSIGNEE"
                if (isAtDock) {
                    val arrivalTime = if (effectiveDockTime != null && effectiveDockTime > 0L) effectiveDockTime else (activeLoad?.pickupTimestamp ?: System.currentTimeMillis())
                    val elapsedMins = maxOf(0L, (System.currentTimeMillis() - arrivalTime) / 60000L)
                    val hours = elapsedMins / 60
                    val mins = elapsedMins % 60
                    val freeMinsLeft = maxOf(0L, 120L - elapsedMins)

                    Surface(
                        color = if (elapsedMins > 120L) Color(0xFFDC2626) else Color(0xFF1E293B), // Red if in detention
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "⏱️ Dock: ${hours}h ${mins}m | Free: ${freeMinsLeft}m",
                            fontSize = 9.sp,
                            color = if (elapsedMins > 120L) Color.White else Color(0xFFF59E0B),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    // Live Odometer Metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("BOUNCE", fontSize = 7.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                            Text("${String.format(Locale.US, "%.1f", effectiveBounce)} mi", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("LOADED", fontSize = 7.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                            Text("${String.format(Locale.US, "%.1f", effectiveLoaded)} mi", fontSize = 10.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 1-Tap Wrist Action Button
                val (btnText, actionCmd) = when (effectiveState) {
                    "ACTIVE_BOUNCE" -> Pair("Arrive Shipper", WearableDataSyncManager.ACTION_ARRIVE_SHIPPER)
                    "ACTIVE_SHIPPER" -> Pair("Depart Shipper", WearableDataSyncManager.ACTION_DEPART_SHIPPER)
                    "ACTIVE_LOADED" -> Pair("Arrive Consignee", WearableDataSyncManager.ACTION_ARRIVE_CONSIGNEE)
                    "ACTIVE_CONSIGNEE" -> Pair("Complete Load", WearableDataSyncManager.ACTION_COMPLETE_LOAD)
                    else -> Pair("Arrive Shipper", WearableDataSyncManager.ACTION_ARRIVE_SHIPPER)
                }

                Button(
                    onClick = { onWristAction(actionCmd) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B00), // Safety Orange
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(30.dp)
                ) {
                    Text(
                        btnText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
