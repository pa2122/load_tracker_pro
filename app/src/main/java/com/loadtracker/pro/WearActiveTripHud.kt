package com.loadtracker.pro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun WearActiveTripHud(
    activeLoad: CurrentLoad?,
    isConnectedToPhone: Boolean = true,
    onWristAction: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)), // Steel Navy Dark
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 18.dp, bottom = 14.dp, start = 18.dp, end = 18.dp),
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
                        .background(
                            if (isConnectedToPhone) Color(0xFF22C55E) else Color(0xFFEF4444),
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isConnectedToPhone) "PHONE CONNECTED" else "DISCONNECTED",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isConnectedToPhone) Color(0xFF86EFAC) else Color(0xFFFCA5A5),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (activeLoad == null || activeLoad.tripState == "COMPLETED") {
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
                        "PRO #${activeLoad.proNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFF59E0B), // Gold
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )

                    val statusText = when (activeLoad.tripState) {
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

                // Live Odometer Metrics
                val bounce = if (activeLoad.bounceMilesEnd > 0) activeLoad.bounceMilesEnd else activeLoad.dispatchedBounceMiles
                val loaded = if (activeLoad.loadedMilesEnd > 0) activeLoad.loadedMilesEnd else activeLoad.dispatchedLoadedMiles

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("BOUNCE", fontSize = 7.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                        Text("${String.format(Locale.US, "%.1f", bounce)} mi", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("LOADED", fontSize = 7.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                        Text("${String.format(Locale.US, "%.1f", loaded)} mi", fontSize = 10.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                    }
                }

                // 1-Tap Wrist Action Button
                val (btnText, actionCmd) = when (activeLoad.tripState) {
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
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .height(32.dp)
                ) {
                    Text(btnText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
