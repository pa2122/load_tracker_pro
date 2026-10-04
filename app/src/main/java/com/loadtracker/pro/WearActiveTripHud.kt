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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale

@Composable
fun WearActiveTripHud(
    activeLoad: CurrentLoad?,
    onWristAction: (String) -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentDiagnosticStep by remember { mutableStateOf(1) } // 1: BT, 2: Phone Node, 3: App Check, 4: Request Data, 5: Check Load DB, 6: Odometer & Dock, 7: Wrist HUD
    var isStepRunning by remember { mutableStateOf(false) }
    var stepMessage by remember { mutableStateOf("1. Tap below to check Bluetooth hardware.") }
    var stepRanSuccess by remember { mutableStateOf(false) }

    var detectedPhoneName by remember { mutableStateOf<String?>(null) }
    var phoneConnectivity by remember { mutableStateOf(WearConnectivityEngine.ConnectionStatus.CONNECTED) }
    var remoteLoadPayload by remember { mutableStateOf<WearableDataSyncManager.WearTripStatePayload?>(null) }

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

    val hasActiveLoad = phoneConnectivity == WearConnectivityEngine.ConnectionStatus.CONNECTED &&
            !effectivePro.isNullOrBlank() && 
            effectiveState != "COMPLETED" && 
            (activeLoad == null || activeLoad.tripState != "COMPLETED")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)), // Steel Navy Dark
        contentAlignment = Alignment.Center
    ) {
        if (currentDiagnosticStep < 7) {
            // USER-DRIVEN STEP-BY-STEP DIAGNOSTIC LOADING SCREEN
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 22.dp, bottom = 28.dp, start = 18.dp, end = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Step Counter
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    val stepLabel = if (currentDiagnosticStep <= 3) "STEP $currentDiagnosticStep / 6" else "DATA STEP $currentDiagnosticStep / 6"
                    Text(
                        text = stepLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF38BDF8),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Step Output Message
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isStepRunning) {
                        CircularProgressIndicator(
                            color = Color(0xFFFF6B00),
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Text(
                        text = stepMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // User Proceed / Run Action Button
                val buttonLabel = when {
                    isStepRunning -> "Running Check..."
                    !stepRanSuccess -> "Run Step $currentDiagnosticStep ➔"
                    currentDiagnosticStep == 6 -> "Open Wrist HUD ➔"
                    else -> "Proceed to Step ${currentDiagnosticStep + 1} ➔"
                }

                Button(
                    onClick = {
                        if (isStepRunning) return@Button

                        if (stepRanSuccess) {
                            // Advance to next step
                            if (currentDiagnosticStep < 6) {
                                currentDiagnosticStep += 1
                                stepRanSuccess = false
                                stepMessage = when (currentDiagnosticStep) {
                                    2 -> "2. Tap to check phone Bluetooth connection."
                                    3 -> "3. Tap to check app on ${detectedPhoneName ?: "phone"}."
                                    4 -> "4A. Tap to send load request to phone."
                                    5 -> "4B. Tap to check active load in database."
                                    6 -> "4C. Tap to verify odometer & dock timers."
                                    else -> ""
                                }
                            } else {
                                // Proceed to Wrist HUD
                                currentDiagnosticStep = 7
                            }
                        } else {
                            // Execute current step check
                            isStepRunning = true
                            scope.launch {
                                when (currentDiagnosticStep) {
                                    1 -> {
                                        val (ok, msg) = WearConnectivityEngine.checkBluetoothStep(ctx)
                                        stepMessage = msg
                                        stepRanSuccess = ok
                                        if (!ok) phoneConnectivity = WearConnectivityEngine.ConnectionStatus.BLUETOOTH_OFF
                                    }
                                    2 -> {
                                        val (phone, msg) = WearConnectivityEngine.checkPhoneConnectionStep(ctx)
                                        stepMessage = msg
                                        detectedPhoneName = phone
                                        stepRanSuccess = phone != null
                                        if (phone == null) phoneConnectivity = WearConnectivityEngine.ConnectionStatus.PHONE_DISCONNECTED
                                    }
                                    3 -> {
                                        val (ok, msg) = WearConnectivityEngine.checkPhoneAppCapabilityStep(ctx, detectedPhoneName)
                                        stepMessage = msg
                                        stepRanSuccess = ok
                                        if (!ok) phoneConnectivity = WearConnectivityEngine.ConnectionStatus.APP_NOT_INSTALLED_ON_PHONE
                                    }
                                    4 -> {
                                        val (ok, msg) = WearConnectivityEngine.requestActiveLoadStep(ctx)
                                        stepMessage = "✓ 4A: Request sent to ${detectedPhoneName ?: "phone"}"
                                        stepRanSuccess = true
                                        phoneConnectivity = WearConnectivityEngine.ConnectionStatus.CONNECTED
                                    }
                                    5 -> {
                                        val proNum = effectivePro
                                        if (!proNum.isNullOrBlank()) {
                                            stepMessage = "✓ 4B: Active Load Found (PRO #$proNum)"
                                        } else {
                                            stepMessage = "📍 4B: No Active Trip Found on Phone"
                                        }
                                        stepRanSuccess = true
                                    }
                                    6 -> {
                                        stepMessage = "✓ 4C: Odometer & Dock Timers Ready"
                                        stepRanSuccess = true
                                    }
                                }
                                isStepRunning = false
                            }
                        }
                    },
                    enabled = !isStepRunning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (stepRanSuccess) Color(0xFF22C55E) else Color(0xFFFF6B00), // Green when success, Orange to run
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(28.dp)
                ) {
                    Text(
                        buttonLabel,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        } else {
            // STEP 7: WRIST HUD DISPLAY
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 28.dp, bottom = 28.dp, start = 20.dp, end = 20.dp), // Comfortably padded top & bottom for circular screens
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Connection Status Badge at Top displaying Connected Phone Name
                val connectedPhone = detectedPhoneName
                val (statusLabel, badgeColor, textColor) = when (phoneConnectivity) {
                    WearConnectivityEngine.ConnectionStatus.BLUETOOTH_OFF -> Triple("BLUETOOTH OFF", Color(0xFFEF4444), Color(0xFFFCA5A5))
                    WearConnectivityEngine.ConnectionStatus.PHONE_DISCONNECTED -> Triple("DISCONNECTED", Color(0xFFEF4444), Color(0xFFFCA5A5))
                    WearConnectivityEngine.ConnectionStatus.APP_NOT_INSTALLED_ON_PHONE -> Triple("INSTALL ON PHONE", Color(0xFFF59E0B), Color(0xFFFDE68A))
                    WearConnectivityEngine.ConnectionStatus.CONNECTED -> Triple(
                        if (!connectedPhone.isNullOrBlank()) "📱 $connectedPhone" else "PHONE CONNECTED",
                        Color(0xFF22C55E),
                        Color(0xFF86EFAC)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(badgeColor, shape = CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor,
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
                        val fallbackTitle = if (phoneConnectivity == WearConnectivityEngine.ConnectionStatus.CONNECTED) "📍 No Active Trip" else "📱 Check Phone"
                        val fallbackSub = when (phoneConnectivity) {
                            WearConnectivityEngine.ConnectionStatus.BLUETOOTH_OFF -> "Turn on Bluetooth on phone & watch."
                            WearConnectivityEngine.ConnectionStatus.PHONE_DISCONNECTED -> "Connect watch to phone via Bluetooth."
                            WearConnectivityEngine.ConnectionStatus.APP_NOT_INSTALLED_ON_PHONE -> "Install Load Tracker Pro on phone."
                            WearConnectivityEngine.ConnectionStatus.CONNECTED -> "Start a load on your phone to track on wrist."
                        }

                        Text(
                            fallbackTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            fallbackSub,
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
                            .height(28.dp)
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
}
