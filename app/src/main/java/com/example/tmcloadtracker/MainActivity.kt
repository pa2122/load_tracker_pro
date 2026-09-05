package com.example.tmcloadtracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.tmcloadtracker.ui.theme.LoadTrackerProTheme
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : ComponentActivity() {

    private val viewModel: LoadViewModel by viewModels()

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { _ -> }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()

        setContent {
            LoadTrackerProTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    var isAppStartingUp by remember { mutableStateOf(value = true) }
                    var loadingStatusText by remember { mutableStateOf(value = "Initializing Engines...") }

                    LaunchedEffect(key1 = Unit) {
                        delay(1200.milliseconds) 
                        loadingStatusText = "Loading Databases..."
                        delay(1000.milliseconds) 
                        isAppStartingUp = false 
                    }
                    var currentScreen by remember { mutableStateOf(value = "dashboard") }
                    var tripToEdit by remember { mutableStateOf<CurrentLoad?>(value = null) }
                    var tripForMap by remember { mutableStateOf<CurrentLoad?>(value = null) }

                    var isProUser by remember { mutableStateOf(value = false) }

                    var defPercent by remember { mutableStateOf(value = "31.0") }
                    var tarp8Pay by remember { mutableStateOf(value = "50.0") }
                    var tarp4Pay by remember { mutableStateOf(value = "30.0") }
                    var homeBase by remember { mutableStateOf(value = "") }
                    var homeRawPaste by remember { mutableStateOf(value = "") }
                    var homeLat by remember { mutableStateOf<Double?>(value = null) }
                    var homeLong by remember { mutableStateOf<Double?>(value = null) }

                    fun resolveHomeAddress(input: String) {
                        val lines = input.lines().filter { it.isNotBlank() }
                        if (lines.isEmpty()) return
                        val addressPart = lines.joinToString(separator = ", ").trim()
                        try {
                            val geocoder = Geocoder(this@MainActivity, Locale.US)
                            @Suppress("DEPRECATION")
                            val results = geocoder.getFromLocationName(addressPart, 1)
                            if (!results.isNullOrEmpty()) {
                                val loc = results[0]
                                homeLat = loc.latitude
                                homeLong = loc.longitude
                                // Also update the display name if first line looks like a label
                                if (lines.size > 1) homeBase = lines[0].trim()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    var isTrainingActive by remember { mutableStateOf(value = false) }
                    var flatTrainerPayRate by remember { mutableStateOf(value = "200.0") }

                    val triggerHelpView = remember { mutableStateOf(value = false) }

                    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                    val drawerScrollState = rememberScrollState()
                    val scope = rememberCoroutineScope()

                    val savedLoads by viewModel.allLoads.collectAsState(initial = emptyList())

                    LaunchedEffect(key1 = savedLoads) {
                        val active = savedLoads.find { 
                            (it.tripState != "COMPLETED") && 
                            (it.tripState != "NOT_STARTED") && 
                            (it.tripState != "PAUSED_AT_HOME") 
                        }
                        if (active != null) {
                            val fineLocation = ContextCompat.checkSelfPermission(
                                this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION,
                            ) == PackageManager.PERMISSION_GRANTED
                            
                            if (fineLocation) {
                                TrackingService.activeSegment = when (active.tripState) {
                                    "ACTIVE_BOUNCE" -> "Bounce"
                                    "ACTIVE_LOADED" -> "Loaded"
                                    else -> "Paused"
                                }
                                TrackingService.activeProNumber = active.proNumber
                                
                                if (active.tripState == "ACTIVE_BOUNCE") {
                                    TrackingService.targetLat = active.shipperLat
                                    TrackingService.targetLong = active.shipperLong
                                    TrackingService.targetName = active.shipperName ?: "Shipper"
                                    TrackingService.isGeofenceActive = active.shipperLat != null
                                } else if (active.tripState == "ACTIVE_LOADED") {
                                    TrackingService.targetLat = active.consigneeLat
                                    TrackingService.targetLong = active.consigneeLong
                                    TrackingService.targetName = active.consigneeName ?: "Consignee"
                                    TrackingService.isGeofenceActive = active.consigneeLat != null
                                }

                                startService(Intent(this@MainActivity, TrackingService::class.java))
                            }
                        }
                    }

                    // 📍 SYNC HOME COORDS TO SERVICE
                    LaunchedEffect(key1 = homeLat, key2 = homeLong) {
                        TrackingService.homeLat = homeLat
                        TrackingService.homeLong = homeLong
                    }

                    val weeklySummary by remember(
                        savedLoads,
                        defPercent,
                        tarp8Pay,
                        tarp4Pay,
                        isTrainingActive,
                        flatTrainerPayRate,
                    ) {
                        derivedStateOf {
                            viewModel.getCurrentWeekSummary(
                                lumberRate = tarp8Pay.toDoubleOrNull() ?: 0.0,
                                steelRate = tarp4Pay.toDoubleOrNull() ?: 0.0,
                                isTraining = isTrainingActive,
                                trainerRate = flatTrainerPayRate.toDoubleOrNull() ?: 0.0
                            )
                        }
                    }

                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet(modifier = Modifier.width(width = 300.dp)) {
                                Column(
                                    modifier = Modifier
                                        .padding(all = 24.dp)
                                        .fillMaxHeight()
                                        .verticalScroll(state = drawerScrollState),
                                    verticalArrangement = Arrangement.spacedBy(space = 16.dp)
                                ) {
                                    Text(
                                        "Configurations",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    OutlinedTextField(
                                        value = defPercent,
                                        onValueChange = { input ->
                                            val filtered =
                                                input.filter { it.isDigit() || it == '.' }
                                            if (filtered.isEmpty() || filtered == ".") defPercent =
                                                filtered
                                            else if (filtered.count { it == '.' } <= 1) {
                                                val value = filtered.toDoubleOrNull()
                                                if ((value != null) && (value >= 0.0) && (value <= 100.0)) {
                                                    defPercent = filtered
                                                }
                                            }
                                        },
                                        label = { Text("Default Pay Rate (%)") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number,
                                            imeAction = ImeAction.Next
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = tarp8Pay,
                                        onValueChange = { input ->
                                            tarp8Pay = input.filter { it.isDigit() || it == '.' }
                                        },
                                        label = { Text("8' Drop Tarp Pay ($)") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number,
                                            imeAction = ImeAction.Next
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = tarp4Pay,
                                        onValueChange = { input ->
                                            tarp4Pay = input.filter { it.isDigit() || it == '.' }
                                        },
                                        label = { Text("4' Drop Tarp Pay ($)") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number,
                                            imeAction = ImeAction.Next
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = homeRawPaste,
                                        onValueChange = { homeRawPaste = it },
                                        label = { Text("Home Address (Paste)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 2
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
                                    ) {
                                        Button(
                                            onClick = { resolveHomeAddress(homeRawPaste) },
                                            enabled = homeRawPaste.isNotBlank(),
                                            modifier = Modifier.weight(weight = 1f)
                                        ) {
                                            Text("Verify Address")
                                        }

                                        Button(
                                            onClick = {
                                                val lastLat = TrackingService.currentLatitude
                                                val lastLong = TrackingService.currentLongitude
                                                if (lastLat != null && lastLong != null) {
                                                    homeLat = lastLat
                                                    homeLong = lastLong
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            ),
                                            modifier = Modifier.weight(weight = 1f)
                                        ) {
                                            Icon(Icons.Default.LocationOn, contentDescription = null)
                                            Spacer(Modifier.width(width = 4.dp))
                                            Text("Pin Current")
                                        }
                                    }

                                    Text(
                                        text = if (homeLat != null) "📍 Home Pinned" else "🏠 Home Not Set",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (homeLat != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )

                                    HorizontalDivider()
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text("Active Training Week")
                                        Switch(
                                            checked = isTrainingActive,
                                            onCheckedChange = { isTrainingActive = it },
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text("Unlock Pro Features (Secret)")
                                        Switch(
                                            checked = isProUser,
                                            onCheckedChange = { isProUser = it },
                                        )
                                    }

                                    OutlinedTextField(
                                        value = flatTrainerPayRate,
                                        onValueChange = { input ->
                                            flatTrainerPayRate =
                                                input.filter { it.isDigit() || it == '.' }
                                        },
                                        label = { Text("Weekly Trainer Bonus ($)") },
                                        singleLine = true,
                                        enabled = isTrainingActive,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = {
                                                scope.launch { drawerState.close() }
                                            }
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    HorizontalDivider()
                                    Button(
                                        onClick = {
                                            scope.launch { drawerState.close() }
                                            currentScreen = "facility_search"
                                        },
                                        enabled = isProUser,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(if (isProUser) "Review Facility Insights" else "Review Facility Insights (Pro)")
                                    }

                                    HorizontalDivider()
                                    Button(
                                        onClick = {
                                            scope.launch { drawerState.close() }
                                            tripForMap = null
                                            currentScreen = "route_map"
                                        },
                                        enabled = isProUser,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(if (isProUser) "View Global Route Heatmap" else "Global Route Heatmap (Pro)")
                                    }

                                    HorizontalDivider()
                                    Button(
                                        onClick = {
                                            scope.launch { drawerState.close() }
                                            exportToCsv()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.tertiary
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Export Payload History (CSV)")
                                    }

                                    HorizontalDivider()
                                    Button(
                                        onClick = {
                                            scope.launch { drawerState.close() }
                                            triggerHelpView.value = true
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Open Driver's Guide")
                                    }

                                    Spacer(modifier = Modifier.weight(weight = 1f))
                                    Button(
                                        onClick = { scope.launch { drawerState.close() } },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Save & Close Menu")
                                    }
                                }
                            }
                        }
                    ) {
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = { Text("Load Tracker Pro") },
                                    navigationIcon = {
                                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                            Icon(
                                                Icons.Default.Menu,
                                                contentDescription = "Open Configurations"
                                            )
                                        }
                                    }
                                )
                            }
                        ) { innerPadding ->
                            Surface(modifier = Modifier.padding(paddingValues = innerPadding)) {
                                if (isAppStartingUp) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(all = 32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(space = 20.dp)
                                        ) {
                                            Text(
                                                text = "Load Tracker Pro",
                                                style = MaterialTheme.typography.headlineLarge,
                                                color = MaterialTheme.colorScheme.primary
                                            )

                                            CircularProgressIndicator(
                                                color = MaterialTheme.colorScheme.primary,
                                                strokeWidth = 4.dp,
                                                modifier = Modifier.size(size = 48.dp)
                                            )

                                            Text(
                                                text = loadingStatusText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }

                                } else {
                                    when (currentScreen) {
                                        "dashboard" -> {
                                            val liveBounce by TrackingService.totalBounceMilesTracked.collectAsState()
                                            val liveLoaded by TrackingService.totalLoadedMilesTracked.collectAsState()

                                            DashboardScreen(
                                                summary = weeklySummary,
                                                pastLoads = savedLoads,
                                                isProUser = isProUser,
                                                onAddNewLoadClick = { 
                                                    tripToEdit = null
                                                    currentScreen = "entry" 
                                                },
                                                onEditTripClick = { load ->
                                                    tripToEdit = load
                                                    currentScreen = "entry"
                                                },
                                                onDeleteTripClick = { load ->
                                                    if (load.tripState != "COMPLETED") {
                                                        stopService(Intent(this@MainActivity, TrackingService::class.java))
                                                        TrackingService.totalBounceMilesTracked.value = 0.0
                                                        TrackingService.totalLoadedMilesTracked.value = 0.0
                                                    }
                                                    viewModel.deleteLoad(load)
                                                },
                                                liveBounceMiles = liveBounce,
                                                liveLoadedMiles = liveLoaded,
                                                isTrainingActive = isTrainingActive,
                                                flatTrainerPayRate = flatTrainerPayRate.toDoubleOrNull()
                                                    ?: 0.0,
                                                showHelpOnLaunch = triggerHelpView.value,
                                                onDismissHelpDialog = {
                                                    triggerHelpView.value = false
                                                },
                                                onViewMapClick = { load ->
                                                    tripForMap = load
                                                    currentScreen = "route_map"
                                                },
                                                onUpdateTripClick = { updatedTripEntity ->
                                                    if (updatedTripEntity.tripState == "COMPLETED") {
                                                        stopService(
                                                            Intent(
                                                                this@MainActivity,
                                                                TrackingService::class.java
                                                            )
                                                        )
                                                        TrackingService.activeProNumber = null
                                                    }
                                                    viewModel.saveLoad(
                                                        proNumber = updatedTripEntity.proNumber,
                                                        dispBounce = updatedTripEntity.dispatchedBounceMiles,
                                                        dispLoaded = updatedTripEntity.dispatchedLoadedMiles,
                                                        bounceStart = updatedTripEntity.bounceMilesStart,
                                                        bounceEnd = updatedTripEntity.bounceMilesEnd,
                                                        loadedStart = updatedTripEntity.loadedMilesStart,
                                                        loadedEnd = updatedTripEntity.loadedMilesEnd,
                                                        ratePercent = updatedTripEntity.percentageRate,
                                                        loadPay = updatedTripEntity.loadPay,
                                                        tarpType = updatedTripEntity.tarpType,
                                                        isPreTarped = updatedTripEntity.isPreTarped,
                                                        pickupTimestamp = updatedTripEntity.pickupTimestamp,
                                                        isGoingHome = updatedTripEntity.isGoingHome,
                                                        tripState = updatedTripEntity.tripState,
                                                        tripNotes = updatedTripEntity.tripNotes,
                                                        deliveryTimestamp = updatedTripEntity.deliveryTimestamp,
                                                        shipperName = updatedTripEntity.shipperName,
                                                        shipperLat = updatedTripEntity.shipperLat,
                                                        shipperLong = updatedTripEntity.shipperLong,
                                                        consigneeName = updatedTripEntity.consigneeName,
                                                        consigneeLat = updatedTripEntity.consigneeLat,
                                                        consigneeLong = updatedTripEntity.consigneeLong
                                                    )
                                                }
                                            )
                                        }

                                        "entry" -> {
                                            LoadEntryScreen(
                                                initialPercentage = defPercent,
                                                editingLoad = tripToEdit,
                                                onSaveClick = { finalizedLoadEntity ->
                                                    if (finalizedLoadEntity.tripState.startsWith(prefix = "ACTIVE")) {
                                                        TrackingService.activeProNumber = finalizedLoadEntity.proNumber
                                                    }
                                                    viewModel.saveLoad(
                                                        proNumber = finalizedLoadEntity.proNumber,
                                                        dispBounce = finalizedLoadEntity.dispatchedBounceMiles,
                                                        dispLoaded = finalizedLoadEntity.dispatchedLoadedMiles,
                                                        bounceStart = finalizedLoadEntity.bounceMilesStart,
                                                        bounceEnd = finalizedLoadEntity.bounceMilesEnd,
                                                        loadedStart = finalizedLoadEntity.loadedMilesStart,
                                                        loadedEnd = finalizedLoadEntity.loadedMilesEnd,
                                                        ratePercent = finalizedLoadEntity.percentageRate,
                                                        loadPay = finalizedLoadEntity.loadPay,
                                                        tarpType = finalizedLoadEntity.tarpType,
                                                        isPreTarped = finalizedLoadEntity.isPreTarped,
                                                        pickupTimestamp = finalizedLoadEntity.pickupTimestamp,
                                                        isGoingHome = finalizedLoadEntity.isGoingHome,
                                                        tripState = finalizedLoadEntity.tripState,
                                                        tripNotes = finalizedLoadEntity.tripNotes,
                                                        deliveryTimestamp = finalizedLoadEntity.deliveryTimestamp,
                                                        shipperName = finalizedLoadEntity.shipperName,
                                                        shipperLat = finalizedLoadEntity.shipperLat,
                                                        shipperLong = finalizedLoadEntity.shipperLong,
                                                        consigneeName = finalizedLoadEntity.consigneeName,
                                                        consigneeLat = finalizedLoadEntity.consigneeLat,
                                                        consigneeLong = finalizedLoadEntity.consigneeLong
                                                    )
                                                    tripToEdit = null
                                                    currentScreen = "dashboard"
                                                },
                                                onCancelClick = {
                                                    tripToEdit = null
                                                    currentScreen = "dashboard"
                                                }
                                            )
                                        }

                                        "facility_search" -> {
                                            FacilitySearchScreen(
                                                viewModel = viewModel,
                                                onBack = { currentScreen = "dashboard" }
                                            )
                                        }

                                        "route_map" -> {
                                            val homeLatLng = if (homeLat != null && homeLong != null) {
                                                LatLng(homeLat!!, homeLong!!)
                                            } else null

                                            tripForMap?.let { load ->
                                                RouteMapScreen(
                                                    load = load,
                                                    viewModel = viewModel,
                                                    homeLocation = homeLatLng,
                                                    onBack = { currentScreen = "dashboard" }
                                                )
                                            } ?: run {
                                                RouteMapScreen(
                                                    load = null,
                                                    viewModel = viewModel,
                                                    homeLocation = homeLatLng,
                                                    onBack = { currentScreen = "dashboard" }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun exportToCsv() {
        val csvContent = viewModel.generateCsvContent()
        val file = File(cacheDir, "Load_Tracker_Export.csv")
        try {
            FileOutputStream(file).use {
                it.write(csvContent.toByteArray())
            }

            val contentUri = FileProvider.getUriForFile(
                this,
                "$packageName.provider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Load Tracker Pro Export")
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share CSV via..."))

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkAndRequestPermissions() {
        val fineLocation = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineLocation || !coarseLocation) {
            val permissions = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            permLauncher.launch(permissions.toTypedArray())
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationPermission = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!notificationPermission) {
                permLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            }
        }
    }
}
