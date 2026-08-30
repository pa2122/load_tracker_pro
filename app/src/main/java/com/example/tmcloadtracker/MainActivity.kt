package com.example.tmcloadtracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: LoadViewModel by viewModels()

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()

        setContent {
            MaterialTheme {
                val useDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
                val chosenColorScheme = if (useDarkTheme) DarkColors else LightColors
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isAppStartingUp by remember { mutableStateOf(true) }
                    var loadingStatusText by remember { mutableStateOf("Initializing Odometer Engines...") }

                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(1200) // Show spinner for 1.2 seconds
                        loadingStatusText = "Loading Saved Trip Database Logs..."
                        kotlinx.coroutines.delay(1000) // Hold state for another 1 second
                        isAppStartingUp = false // Shuts down splash view, boots your home screen
                    }
                    var currentScreen by remember { mutableStateOf("dashboard") }

                    var defPercent by remember { mutableStateOf("31.0") }
                    var lTarpPay by remember { mutableStateOf("50.0") }
                    var sTarpPay by remember { mutableStateOf("30.0") }
                    var homeBase by remember { mutableStateOf("") }

                    var isTrainingActive by remember { mutableStateOf(false) }
                    var flatTrainerPayRate by remember { mutableStateOf("200.0") }

                    var triggerHelpView = remember { mutableStateOf(false) }

                    val drawerState = rememberDrawerState(DrawerValue.Closed)
                    val scope = rememberCoroutineScope()

                    val savedLoads by viewModel.allLoads.collectAsState(initial = emptyList())

                    // 📍 THE ADAPTIVE STATE ENGINE BLOCK CALCULATION CONTEXT LINK
                    val weeklySummary by remember(
                        savedLoads,
                        defPercent,
                        lTarpPay,
                        sTarpPay,
                        isTrainingActive,
                        flatTrainerPayRate
                    ) {
                        derivedStateOf {
                            viewModel.getCurrentWeekSummary(
                                lumberRate = lTarpPay.toDoubleOrNull() ?: 0.0,
                                steelRate = sTarpPay.toDoubleOrNull() ?: 0.0,
                                isTraining = isTrainingActive,
                                trainerRate = flatTrainerPayRate.toDoubleOrNull() ?: 0.0
                            )
                        }
                    }

                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                                Column(
                                    modifier = Modifier
                                        .padding(24.dp)
                                        .fillMaxHeight(),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
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
                                                if (value != null && value >= 0.0 && value <= 100.0) defPercent =
                                                    filtered
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
                                        value = lTarpPay,
                                        onValueChange = { input ->
                                            lTarpPay = input.filter { it.isDigit() || it == '.' }
                                        },
                                        label = { Text("Lumber Tarp Pay ($)") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number,
                                            imeAction = ImeAction.Next
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = sTarpPay,
                                        onValueChange = { input ->
                                            sTarpPay = input.filter { it.isDigit() || it == '.' }
                                        },
                                        label = { Text("Steel Tarp Pay ($)") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number,
                                            imeAction = ImeAction.Next
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = homeBase,
                                        onValueChange = { homeBase = it },
                                        label = { Text("Home Base (City, ST)") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Text,
                                            imeAction = ImeAction.Next
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    HorizontalDivider()
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Active Training Week")
                                        Switch(
                                            checked = isTrainingActive,
                                            onCheckedChange = { isTrainingActive = it })
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
                                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                            onDone = { scope.launch { drawerState.close() } }),
                                        modifier = Modifier.fillMaxWidth()
                                    )
// 📍 PLACE THIS BUTTON INSIDE YOUR SIDEBAR SHEET VERTICAL COLUMN
                                    HorizontalDivider()
                                    Button(
                                        onClick = {
                                            // We temporarily close the slide menu and notify the screen state engine
                                            scope.launch { drawerState.close() }
                                            triggerHelpView.value = true
                                            // To make it easy, we can toggle our state or pass a signal

                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Open Built-In Reference Notes")
                                    }

                                    Spacer(modifier = Modifier.weight(1f))
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
                                    title = { Text("TMC Load Tracker") },
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
                            Surface(modifier = Modifier.padding(innerPadding)) {
                                if (isAppStartingUp) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(20.dp)
                                        ) {
                                            Text(
                                                text = "TMC Load Tracker",
                                                style = MaterialTheme.typography.headlineLarge,
                                                color = MaterialTheme.colorScheme.primary
                                            )

                                            // Native Material Design 3 spinning loading icon wheel
                                            CircularProgressIndicator(
                                                color = MaterialTheme.colorScheme.primary,
                                                strokeWidth = 4.dp,
                                                modifier = Modifier.size(48.dp)
                                            )

                                            // 📍 Live status text row that changes dynamically as background steps load
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

                                            //
                                            // var triggerHelpView by remember { mutableStateOf(false) }

                                            DashboardScreen(
                                                summary = weeklySummary,
                                                pastLoads = savedLoads,
                                                onAddNewLoadClick = { currentScreen = "entry" },
                                                liveBounceMiles = liveBounce,
                                                liveLoadedMiles = liveLoaded,
                                                isTrainingActive = isTrainingActive,
                                                flatTrainerPayRate = flatTrainerPayRate.toDoubleOrNull()
                                                    ?: 0.0,
                                                showHelpOnLaunch = triggerHelpView.value,
                                                onDismissHelpDialog = {
                                                    triggerHelpView.value = false
                                                },
                                                onUpdateTripClick = { updatedTripEntity ->
                                                    if (updatedTripEntity.tripState == "COMPLETED") {
                                                        stopService(
                                                            Intent(
                                                                this@MainActivity,
                                                                TrackingService::class.java
                                                            )
                                                        )
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
                                                        tripState = updatedTripEntity.tripState
                                                    )
                                                }
                                            )
                                        }

                                        "entry" -> {
                                            LoadEntryScreen(
                                                initialPercentage = defPercent,
                                                onSaveClick = { finalizedLoadEntity ->
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
                                                        tripState = finalizedLoadEntity.tripState
                                                    )
                                                    currentScreen = "dashboard"
                                                },
                                                onCancelClick = { currentScreen = "dashboard" }
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
    // 📍 CHUNK A: COLOR SCHEME SCHEMATICS DEFINITIONS FOR LIGHT/DARK MODES
    private val LightColors = androidx.compose.material3.lightColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFF005FAF), // TMC Royal Blue Accent
        secondary = androidx.compose.ui.graphics.Color(0xFF535F70),
        tertiary = androidx.compose.ui.graphics.Color(0xFF006A60),
        background = androidx.compose.ui.graphics.Color(0xFFFDFDFD),
        surface = androidx.compose.ui.graphics.Color(0xFFFDFDFD)
    )

    private val DarkColors = androidx.compose.material3.darkColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFFA4C8FF), // Softer Blue for Night Vision Preservation
        secondary = androidx.compose.ui.graphics.Color(0xFFBBC7DB),
        tertiary = androidx.compose.ui.graphics.Color(0xFF82D3C4),
        background = androidx.compose.ui.graphics.Color(0xFF1A1C1E), // Deep Charcoal background
        surface = androidx.compose.ui.graphics.Color(0xFF1A1C1E)
    )


    private fun checkAndRequestPermissions() {
        val fineLocation = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineLocation || !coarseLocation) {
            permLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
}
