package com.loadtracker.pro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
    onWristAction: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)), // Steel Navy Dark
        contentAlignment = Alignment.Center
    ) {
        if (activeLoad == null || activeLoad.tripState == "COMPLETED") {
            // NO ACTIVE TRIP SCREEN
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "📍 No Active Trip",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Start a load on your phone to track live miles & detention on wrist.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8), // Slate Gray
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp
                )
            }
        } else {
            // ACTIVE TRIP WRIST HUD SCREEN
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header: PRO # & Status
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            statusText,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Live Odometer Metrics
                val bounce = if (activeLoad.bounceMilesEnd > 0) activeLoad.bounceMilesEnd else activeLoad.dispatchedBounceMiles
                val loaded = if (activeLoad.loadedMilesEnd > 0) activeLoad.loadedMilesEnd else activeLoad.dispatchedLoadedMiles
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Bounce: ${String.format(Locale.US, "%.1f", bounce)} mi",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        fontSize = 11.sp
                    )
                    Text(
                        "Loaded: ${String.format(Locale.US, "%.1f", loaded)} mi",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF38BDF8), // Light Blue
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
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
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(36.dp)
                ) {
                    Text(btnText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
