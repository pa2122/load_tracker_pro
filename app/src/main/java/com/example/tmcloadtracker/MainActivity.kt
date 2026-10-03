package com.example.tmcloadtracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import android.net.TrafficStats
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.ui.text.font.FontWeight
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.ZoneId
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import dev.jeziellago.compose.markdowntext.MarkdownText
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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

                    val appVersionName = remember {
                        try {
                            @Suppress("DEPRECATION")
                            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
                        } catch (_: Exception) {
                            "1.0.0"
                        }
                    }

                    LaunchedEffect(key1 = Unit) {
                        delay(1200.milliseconds) 
                        loadingStatusText = "Loading Databases..."
                        delay(1000.milliseconds) 
                        isAppStartingUp = false 
                    }
                    var currentScreen by remember { mutableStateOf(value = "dashboard") }
                    var tripToEdit by remember { mutableStateOf<CurrentLoad?>(value = null) }
                    var tripForMap by remember { mutableStateOf<CurrentLoad?>(value = null) }

                    var isProUser by remember { mutableStateOf(value = true) }

                    val devPrefs = remember { getSharedPreferences("dev_prefs", MODE_PRIVATE) }

                    val initDefPercent = remember { devPrefs.getString("def_percent", "31.0") ?: "31.0" }
                    val initTarp8 = remember { devPrefs.getString("tarp_8_pay", "50.0") ?: "50.0" }
                    val initTarp4 = remember { devPrefs.getString("tarp_4_pay", "40.0") ?: "40.0" }
                    val initExtraStop = remember { devPrefs.getString("extra_stop_pay", "0.0") ?: "0.0" }
                    val initHomeRaw = remember { devPrefs.getString("home_raw_paste", "15381 TX-198, Mabank, TX 75147") ?: "15381 TX-198, Mabank, TX 75147" }
                    val initIsTraining = remember { devPrefs.getBoolean("is_training_active", false) }
                    val initTrainerRate = remember { devPrefs.getString("flat_trainer_pay_rate", "200.0") ?: "200.0" }

                    var defPercent by remember { mutableStateOf(initDefPercent) }
                    var tarp8Pay by remember { mutableStateOf(initTarp8) }
                    var tarp4Pay by remember { mutableStateOf(initTarp4) }
                    var extraStopPay by remember { mutableStateOf(initExtraStop) }
                    var homeBase by remember { mutableStateOf(initHomeRaw) }
                    var homeRawPaste by remember { mutableStateOf(initHomeRaw) }
                    var homeLat by remember { mutableStateOf<Double?>(value = 32.3021) }
                    var homeLong by remember { mutableStateOf<Double?>(value = -96.1116) }
                    var isHomeVerified by remember { mutableStateOf(value = true) }

                    var savedDefPercent by remember { mutableStateOf(initDefPercent) }
                    var savedTarp8Pay by remember { mutableStateOf(initTarp8) }
                    var savedTarp4Pay by remember { mutableStateOf(initTarp4) }
                    var savedExtraStopPay by remember { mutableStateOf(initExtraStop) }
                    var savedHomeRawPaste by remember { mutableStateOf(initHomeRaw) }
                    var savedIsTrainingActive by remember { mutableStateOf(initIsTraining) }
                    var savedFlatTrainerPayRate by remember { mutableStateOf(initTrainerRate) }

                    var showDevOptionsDialog by remember { mutableStateOf(value = false) }
                    var showDevAccessRequestDialog by remember { mutableStateOf(value = false) }
                    var showTesterFeedbackDialog by remember { mutableStateOf(value = false) }
                    var showTrainerSettingsDialog by remember { mutableStateOf(value = false) }
                    var showProUpgradeDialog by remember { mutableStateOf(value = false) }
                    var showReadmeDialog by remember { mutableStateOf(value = false) }
                    var isHistoricalEntry by remember { mutableStateOf(value = false) }

                    var initialRxBytes by remember { mutableLongStateOf(value = TrafficStats.getUidRxBytes(Process.myUid())) }
                    var initialTxBytes by remember { mutableLongStateOf(value = TrafficStats.getUidTxBytes(Process.myUid())) }
                    var savedTraineeTier by remember { mutableStateOf(value = devPrefs.getString("trainee_tier", "inexperienced") ?: "inexperienced") }
                    var savedTrainingWeek by remember { mutableIntStateOf(value = devPrefs.getInt("training_week", 1)) }
                    var savedIsTmcBoostActive by remember { mutableStateOf(value = devPrefs.getBoolean("is_tmc_boost_active", true)) }

                    val currentDeviceId = remember {
                        try {
                            Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
                        } catch (_: Exception) {
                            "unknown"
                        }
                    }

                    var isDeviceAuthorized by remember {
                        val savedAuthorizedDevices = devPrefs.getStringSet("authorized_devices", emptySet()) ?: emptySet()
                        mutableStateOf(
                            BuildConfig.DEBUG || savedAuthorizedDevices.contains(currentDeviceId)
                        )
                    }
                    var newTesterDeviceId by remember { mutableStateOf(value = "") }

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
                                isHomeVerified = true
                                if (lines.size > 1) homeBase = lines[0].trim()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    LaunchedEffect(isProUser) {
                        TrackingService.isProUser = isProUser
                    }

                    LaunchedEffect(currentDeviceId) {
                        withContext(Dispatchers.IO) {
                            try {
                                val url = URL("https://raw.githubusercontent.com/pa2122/load_tracker_pro/master/authorized_devs.json")
                                val conn = url.openConnection() as HttpURLConnection
                                conn.requestMethod = "GET"
                                conn.connectTimeout = 5000
                                conn.readTimeout = 5000
                                if (conn.responseCode == 200) {
                                    val jsonText = conn.inputStream.bufferedReader().use { it.readText() }
                                    val array = JSONArray(jsonText)
                                    val remoteIds = mutableSetOf<String>()
                                    for (i in 0 until array.length()) {
                                        remoteIds.add(array.getString(i).trim())
                                    }
                                    withContext(Dispatchers.Main) {
                                        if (remoteIds.contains(currentDeviceId) || BuildConfig.DEBUG) {
                                            isDeviceAuthorized = true
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    LaunchedEffect(Unit) {
                        resolveHomeAddress("15381 TX-198, Mabank, TX 75147")
                    }

                    var isTrainingActive by remember { mutableStateOf(value = false) }
                    var flatTrainerPayRate by remember { mutableStateOf(value = "200.0") }

                    val calculatedTrainerPay = remember(isTrainingActive, savedTraineeTier, savedTrainingWeek, savedIsTmcBoostActive, flatTrainerPayRate) {
                        if (!isTrainingActive) 0.0
                        else {
                            val base = calculateBaseTrainerPay(savedTraineeTier, savedTrainingWeek, flatTrainerPayRate.toDoubleOrNull() ?: 200.0)
                            val boost = if (savedIsTmcBoostActive && savedTraineeTier != "custom") 100.0 else 0.0
                            base + boost
                        }
                    }

                    val triggerHelpView = remember { mutableStateOf(value = false) }

                    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                    val drawerScrollState = rememberScrollState()
                    val scope = rememberCoroutineScope()

                    val savedLoads by viewModel.allLoads.collectAsState(initial = emptyList())

                    val currentTargetFriday = remember {
                        getPayPeriodDate(System.currentTimeMillis())
                    }

                    val currentWeekHasTrainingLoad = remember(savedLoads, currentTargetFriday) {
                        savedLoads.any { load ->
                            (getPayPeriodDate(load.pickupTimestamp) == currentTargetFriday) && load.isTrainingWeek
                        }
                    }

                    LaunchedEffect(key1 = currentWeekHasTrainingLoad) {
                        if (currentWeekHasTrainingLoad) {
                            isTrainingActive = true
                            savedIsTrainingActive = true
                        }
                    }

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

                                ContextCompat.startForegroundService(
                                    this@MainActivity,
                                    Intent(this@MainActivity, TrackingService::class.java)
                                )
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
                        calculatedTrainerPay,
                    ) {
                        derivedStateOf {
                            viewModel.getCurrentWeekSummary(
                                lumberRate = tarp8Pay.toDoubleOrNull() ?: 0.0,
                                steelRate = tarp4Pay.toDoubleOrNull() ?: 0.0,
                                isTrainingGlobal = isTrainingActive,
                                trainerRate = calculatedTrainerPay
                            )
                        }
                    }

                    val hasDrawerSettingsChanged = (defPercent != savedDefPercent) ||
                            (tarp8Pay != savedTarp8Pay) ||
                            (tarp4Pay != savedTarp4Pay) ||
                            (extraStopPay != savedExtraStopPay) ||
                            (homeRawPaste != savedHomeRawPaste) ||
                            (isTrainingActive != savedIsTrainingActive) ||
                            (flatTrainerPayRate != savedFlatTrainerPayRate)

                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        gesturesEnabled = currentScreen != "route_map",
                        drawerContent = {
                            ModalDrawerSheet(modifier = Modifier.width(width = 300.dp)) {
                                Column(
                                    modifier = Modifier
                                        .padding(all = 24.dp)
                                        .fillMaxHeight()
                                        .verticalScroll(state = drawerScrollState),
                                    verticalArrangement = Arrangement.spacedBy(space = 16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Configurations",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        if (hasDrawerSettingsChanged) {
                                            IconButton(
                                                onClick = {
                                                    savedDefPercent = defPercent
                                                    savedTarp8Pay = tarp8Pay
                                                    savedTarp4Pay = tarp4Pay
                                                    savedExtraStopPay = extraStopPay
                                                    savedHomeRawPaste = homeRawPaste
                                                    savedIsTrainingActive = isTrainingActive
                                                    savedFlatTrainerPayRate = flatTrainerPayRate
                                                    devPrefs.edit().apply {
                                                        putString("def_percent", defPercent)
                                                        putString("tarp_8_pay", tarp8Pay)
                                                        putString("tarp_4_pay", tarp4Pay)
                                                        putString("extra_stop_pay", extraStopPay)
                                                        putString("home_raw_paste", homeRawPaste)
                                                        putBoolean("is_training_active", isTrainingActive)
                                                        putString("flat_trainer_pay_rate", flatTrainerPayRate)
                                                        apply()
                                                    }
                                                    resolveHomeAddress(homeRawPaste)
                                                    scope.launch { drawerState.close() }
                                                }
                                            ) {
                                                Icon(
                                                    Icons.Default.Done,
                                                    contentDescription = "Save and Close Menu",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }

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
                                        value = extraStopPay,
                                        onValueChange = { input ->
                                            extraStopPay = input.filter { it.isDigit() || it == '.' }
                                        },
                                        label = { Text("Extra Stop Pay ($)") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number,
                                            imeAction = ImeAction.Next
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = homeRawPaste,
                                        onValueChange = {
                                            homeRawPaste = it
                                            isHomeVerified = false
                                            homeLat = null
                                            homeLong = null
                                        },
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
                                            enabled = homeRawPaste.isNotBlank() && !isHomeVerified,
                                            modifier = Modifier.weight(weight = 1f)
                                        ) {
                                            Text(if (isHomeVerified) "Verified ✅" else "Verify Address")
                                        }

                                        Button(
                                            onClick = {
                                                val lastLat = TrackingService.currentLatitude
                                                val lastLong = TrackingService.currentLongitude
                                                if (lastLat != null && lastLong != null) {
                                                    homeLat = lastLat
                                                    homeLong = lastLong
                                                    isHomeVerified = true
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
                                    Text("🎓 Trainer Incentive Settings", style = MaterialTheme.typography.titleSmall)
                                    Surface(
                                        color = if (isTrainingActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = if (isTrainingActive) "Status: ACTIVE 🟢" else "Status: INACTIVE 🔴",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isTrainingActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (isTrainingActive) {
                                                val tierLabel = when (savedTraineeTier) {
                                                    "inexperienced" -> "Inexperienced (Wk $savedTrainingWeek of 4)"
                                                    "experienced" -> "Experienced (Wk $savedTrainingWeek of 2)"
                                                    else -> "Custom Rate"
                                                }
                                                Text(tierLabel, style = MaterialTheme.typography.bodySmall)
                                                Text(
                                                    "Added: +$${String.format(Locale.US, "%.2f", calculatedTrainerPay)}/wk",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            Button(
                                                onClick = {
                                                    scope.launch { drawerState.close() }
                                                    showTrainerSettingsDialog = true
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.secondary,
                                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("⚙️ Configure Trainer Pay")
                                            }
                                        }
                                    }

                                    HorizontalDivider()
                                    Button(
                                        onClick = {
                                            scope.launch { drawerState.close() }
                                            if (isProUser) {
                                                currentScreen = "facility_search"
                                            } else {
                                                showProUpgradeDialog = true
                                            }
                                        },
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
                                            if (isProUser) {
                                                tripForMap = null
                                                currentScreen = "route_map"
                                            } else {
                                                showProUpgradeDialog = true
                                            }
                                        },
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
                                            showTesterFeedbackDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null)
                                        Spacer(modifier = Modifier.width(width = 6.dp))
                                        Text("🐛 Report Bug / Feedback")
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

                                    HorizontalDivider()
                                    Button(
                                        onClick = {
                                            scope.launch { drawerState.close() }
                                            showReadmeDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Info, contentDescription = null)
                                        Spacer(modifier = Modifier.width(width = 6.dp))
                                        Text("📖 App Technical Specs (README)")
                                    }

                                    val hasActiveTrip = savedLoads.any { it.tripState != "COMPLETED" && it.tripState != "NOT_STARTED" }
                                    if (hasActiveTrip) {
                                        HorizontalDivider()
                                        Button(
                                            onClick = {
                                                scope.launch { drawerState.close() }
                                                tripToEdit = null
                                                isHistoricalEntry = true
                                                currentScreen = "entry"
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null)
                                            Spacer(modifier = Modifier.width(width = 6.dp))
                                            Text("➕ Add Historical Completed Load")
                                        }
                                    }

                                    if (isDeviceAuthorized || BuildConfig.DEBUG) {
                                        Spacer(modifier = Modifier.weight(weight = 1f))
                                        Button(
                                            onClick = { showDevOptionsDialog = true },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Build, contentDescription = null)
                                            Spacer(modifier = Modifier.width(width = 6.dp))
                                            Text("🛠️ Developer Options")
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(weight = 1f))
                                        Button(
                                            onClick = {
                                                scope.launch { drawerState.close() }
                                                showDevAccessRequestDialog = true
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = null)
                                            Spacer(modifier = Modifier.width(width = 6.dp))
                                            Text("📱 Request Developer Access")
                                        }
                                    }
                                }
                            }
                        }
                    ) {
                        val configuration = LocalConfiguration.current
                        val isTablet = configuration.screenWidthDp >= 600

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
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues = innerPadding)
                            ) {
                                if (isTablet && !isAppStartingUp) {
                                    NavigationRail(
                                        modifier = Modifier.padding(top = 8.dp)
                                    ) {
                                        NavigationRailItem(
                                            selected = currentScreen == "dashboard",
                                            onClick = { currentScreen = "dashboard" },
                                            icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                                            label = { Text("Dashboard") }
                                        )
                                        NavigationRailItem(
                                            selected = currentScreen == "entry",
                                            onClick = {
                                                tripToEdit = null
                                                currentScreen = "entry"
                                            },
                                            icon = { Icon(Icons.Default.Add, contentDescription = "New Load") },
                                            label = { Text("New Load") }
                                        )
                                        NavigationRailItem(
                                            selected = currentScreen == "route_map",
                                            onClick = {
                                                tripForMap = null
                                                currentScreen = "route_map"
                                            },
                                            icon = { Icon(Icons.Default.LocationOn, contentDescription = "Heatmap") },
                                            label = { Text("Heatmap") }
                                        )
                                        NavigationRailItem(
                                            selected = currentScreen == "facility_search",
                                            onClick = { currentScreen = "facility_search" },
                                            icon = { Icon(Icons.Default.Search, contentDescription = "Facilities") },
                                            label = { Text("Facilities") }
                                        )
                                        NavigationRailItem(
                                            selected = false,
                                            onClick = { showTesterFeedbackDialog = true },
                                            icon = { Icon(Icons.Default.Edit, contentDescription = "Feedback") },
                                            label = { Text("Feedback") }
                                        )
                                        if (isDeviceAuthorized || BuildConfig.DEBUG) {
                                            NavigationRailItem(
                                                selected = false,
                                                onClick = { showDevOptionsDialog = true },
                                                icon = { Icon(Icons.Default.Build, contentDescription = "Dev") },
                                                label = { Text("Dev Mode") }
                                            )
                                        }
                                    }
                                }

                                Box(modifier = Modifier.weight(1f)) {
                                    Surface(modifier = Modifier.fillMaxSize()) {
                                if (isAppStartingUp) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(all = 32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(space = 12.dp)
                                        ) {
                                            Image(
                                                painter = painterResource(id = R.drawable.app_logo),
                                                contentDescription = "Load Tracker Pro Logo",
                                                modifier = Modifier
                                                    .size(110.dp)
                                                    .clip(CircleShape)
                                            )

                                            Text(
                                                text = "Load Tracker Pro",
                                                style = MaterialTheme.typography.headlineLarge,
                                                color = MaterialTheme.colorScheme.primary
                                            )

                                            Text(
                                                text = "Version $appVersionName",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))

                                            CircularProgressIndicator(
                                                color = MaterialTheme.colorScheme.primary,
                                                strokeWidth = 3.dp,
                                                modifier = Modifier.size(size = 36.dp)
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
                                                        TrackingService.resetTrackingState()
                                                    }
                                                    viewModel.deleteLoad(load)
                                                },
                                                liveBounceMiles = liveBounce,
                                                liveLoadedMiles = liveLoaded,
                                                isTrainingActive = isTrainingActive,
                                                flatTrainerPayRate = calculatedTrainerPay,
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
                                                        TrackingService.resetTrackingState()
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
                                                        consigneeLong = updatedTripEntity.consigneeLong,
                                                        isTrainingWeek = updatedTripEntity.isTrainingWeek,
                                                        trainerPayRate = updatedTripEntity.trainerPayRate,
                                                        pickupApptText = updatedTripEntity.pickupApptText,
                                                        pickupApptTimestamp = updatedTripEntity.pickupApptTimestamp,
                                                        pickupApptType = updatedTripEntity.pickupApptType,
                                                        consigneeApptText = updatedTripEntity.consigneeApptText,
                                                        consigneeApptTimestamp = updatedTripEntity.consigneeApptTimestamp,
                                                        consigneeApptType = updatedTripEntity.consigneeApptType,
                                                        dockArrivalTime = updatedTripEntity.dockArrivalTime,
                                                        detentionHoursLogged = updatedTripEntity.detentionHoursLogged,
                                                        detentionFlatPay = updatedTripEntity.detentionFlatPay
                                                    )
                                                }
                                            )
                                        }

                                        "entry" -> {
                                            LoadEntryScreen(
                                                initialPercentage = defPercent,
                                                initialIsTraining = isTrainingActive,
                                                initialTrainerPayRate = calculatedTrainerPay,
                                                isProUser = isProUser,
                                                editingLoad = tripToEdit,
                                                isHistorical = isHistoricalEntry,
                                                existingProNumbers = savedLoads.map { it.proNumber },
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
                                                        tripState = if (isHistoricalEntry) "COMPLETED" else finalizedLoadEntity.tripState,
                                                        tripNotes = finalizedLoadEntity.tripNotes,
                                                        deliveryTimestamp = if (isHistoricalEntry) finalizedLoadEntity.pickupTimestamp + 86400000L else finalizedLoadEntity.deliveryTimestamp,
                                                        shipperName = finalizedLoadEntity.shipperName,
                                                        shipperLat = finalizedLoadEntity.shipperLat,
                                                        shipperLong = finalizedLoadEntity.shipperLong,
                                                        consigneeName = finalizedLoadEntity.consigneeName,
                                                        consigneeLat = finalizedLoadEntity.consigneeLat,
                                                        consigneeLong = finalizedLoadEntity.consigneeLong,
                                                        isTrainingWeek = finalizedLoadEntity.isTrainingWeek,
                                                        trainerPayRate = finalizedLoadEntity.trainerPayRate,
                                                        pickupApptText = finalizedLoadEntity.pickupApptText,
                                                        pickupApptTimestamp = finalizedLoadEntity.pickupApptTimestamp,
                                                        pickupApptType = finalizedLoadEntity.pickupApptType,
                                                        consigneeApptText = finalizedLoadEntity.consigneeApptText,
                                                        consigneeApptTimestamp = finalizedLoadEntity.consigneeApptTimestamp,
                                                        consigneeApptType = finalizedLoadEntity.consigneeApptType,
                                                        dockArrivalTime = finalizedLoadEntity.dockArrivalTime,
                                                        detentionHoursLogged = finalizedLoadEntity.detentionHoursLogged,
                                                        detentionFlatPay = finalizedLoadEntity.detentionFlatPay
                                                    )
                                                    tripToEdit = null
                                                    isHistoricalEntry = false
                                                    currentScreen = "dashboard"
                                                },
                                                onCancelClick = {
                                                    tripToEdit = null
                                                    isHistoricalEntry = false
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

                                        "dev_notes" -> {
                                            DevNotesScreen(
                                                onBack = { currentScreen = "dashboard" }
                                            )
                                        }

                                        "route_map" -> {
                                            BackHandler(enabled = true) {
                                                currentScreen = "dashboard"
                                            }
                                            val homeLatLng = if (homeLat != null && homeLong != null) {
                                                LatLng(homeLat!!, homeLong!!)
                                            } else null

                                            RouteMapScreen(
                                                load = tripForMap,
                                                viewModel = viewModel,
                                                homeLocation = homeLatLng,
                                                onBack = { currentScreen = "dashboard" }
                                            )
                                        }
                                    }
                                }
                            }

                    if (showDevOptionsDialog) {
                        AlertDialog(
                            onDismissRequest = { showDevOptionsDialog = false },
                            title = { Text("🛠️ Developer Options") },
                            text = {
                                val devScrollState = rememberScrollState()
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(space = 10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(devScrollState)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text("Developer Mode (Pro Unlocked)", style = MaterialTheme.typography.bodyMedium)
                                        Switch(
                                            checked = isProUser,
                                            onCheckedChange = { isProUser = it },
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            showDevOptionsDialog = false
                                            scope.launch { drawerState.close() }
                                            currentScreen = "dev_notes"
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null)
                                        Spacer(modifier = Modifier.width(width = 6.dp))
                                        Text("Developer Notes & Bug Tracker")
                                    }

                                    HorizontalDivider()

                                    Text("📊 Live Network & GPS Telemetry", style = MaterialTheme.typography.titleSmall)

                                    val currentRx = TrafficStats.getUidRxBytes(Process.myUid())
                                    val currentTx = TrafficStats.getUidTxBytes(Process.myUid())
                                    val sessionRxMb = if (currentRx >= initialRxBytes) (currentRx - initialRxBytes) / (1024.0 * 1024.0) else 0.0
                                    val sessionTxMb = if (currentTx >= initialTxBytes) (currentTx - initialTxBytes) / (1024.0 * 1024.0) else 0.0
                                    val sessionTotalMb = sessionRxMb + sessionTxMb

                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text("Network Data (This Session):", style = MaterialTheme.typography.labelMedium)
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Downloaded (Rx):", style = MaterialTheme.typography.bodySmall)
                                                Text("${String.format(Locale.US, "%.2f", sessionRxMb)} MB", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                            }
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Uploaded (Tx):", style = MaterialTheme.typography.bodySmall)
                                                Text("${String.format(Locale.US, "%.2f", sessionTxMb)} MB", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                            }
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Total Data Used:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                Text("${String.format(Locale.US, "%.2f", sessionTotalMb)} MB", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))
                                            Button(
                                                onClick = {
                                                    initialRxBytes = TrafficStats.getUidRxBytes(Process.myUid())
                                                    initialTxBytes = TrafficStats.getUidTxBytes(Process.myUid())
                                                    Toast.makeText(this@MainActivity, "Data Counter Reset to 0.00 MB!", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("🔄 Reset Data Counter")
                                            }

                                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                            Text("GPS Tracking Telemetry:", style = MaterialTheme.typography.labelMedium)
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Active Segment:", style = MaterialTheme.typography.bodySmall)
                                                Text(TrackingService.activeSegment, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("PRO Number:", style = MaterialTheme.typography.bodySmall)
                                                Text(TrackingService.activeProNumber ?: "None", style = MaterialTheme.typography.bodySmall)
                                            }
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Geofence Target:", style = MaterialTheme.typography.bodySmall)
                                                Text(if (TrackingService.isGeofenceActive) "${TrackingService.targetName} 📍" else "Inactive", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }

                                    HorizontalDivider()

                                    Text("Device Authorization", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "Device ID: $currentDeviceId",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )

                                    Button(
                                        onClick = {
                                            val currentSet = devPrefs.getStringSet("authorized_devices", emptySet())?.toMutableSet() ?: mutableSetOf()
                                            if (isDeviceAuthorized && !BuildConfig.DEBUG) {
                                                currentSet.remove(currentDeviceId)
                                                isDeviceAuthorized = false
                                                Toast.makeText(this@MainActivity, "Device Deauthorized", Toast.LENGTH_SHORT).show()
                                            } else {
                                                currentSet.add(currentDeviceId)
                                                isDeviceAuthorized = true
                                                Toast.makeText(this@MainActivity, "This Device Authorized for Dev Mode!", Toast.LENGTH_SHORT).show()
                                            }
                                            devPrefs.edit().putStringSet("authorized_devices", currentSet).apply()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isDeviceAuthorized) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = if (isDeviceAuthorized) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(if (isDeviceAuthorized) "✅ Device Authorized (Tap to Toggle)" else "📱 Authorize This Device")
                                    }

                                    OutlinedTextField(
                                        value = newTesterDeviceId,
                                        onValueChange = { newTesterDeviceId = it },
                                        label = { Text("Authorize Tester Device ID") },
                                        placeholder = { Text("Paste tester Device ID...") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    if (newTesterDeviceId.isNotBlank()) {
                                        Button(
                                            onClick = {
                                                val currentSet = devPrefs.getStringSet("authorized_devices", emptySet())?.toMutableSet() ?: mutableSetOf()
                                                currentSet.add(newTesterDeviceId.trim())
                                                devPrefs.edit().putStringSet("authorized_devices", currentSet).apply()
                                                Toast.makeText(this@MainActivity, "Authorized Tester Device: ${newTesterDeviceId.trim()}", Toast.LENGTH_SHORT).show()
                                                newTesterDeviceId = ""
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("➕ Authorize Tester Device")
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showDevOptionsDialog = false }) {
                                    Text("Close")
                                }
                            }
                        )
                    }

                    if (showTesterFeedbackDialog) {
                        TesterFeedbackDialog(
                            onDismiss = { showTesterFeedbackDialog = false }
                        )
                    }

                    if (showTrainerSettingsDialog) {
                        TrainerSettingsDialog(
                            initialIsActive = isTrainingActive,
                            initialTier = savedTraineeTier,
                            initialWeek = savedTrainingWeek,
                            initialIsBoostActive = savedIsTmcBoostActive,
                            initialCustomRate = flatTrainerPayRate,
                            onSave = { active, tier, week, boost, customRate, _ ->
                                isTrainingActive = active
                                savedIsTrainingActive = active
                                savedTraineeTier = tier
                                savedTrainingWeek = week
                                savedIsTmcBoostActive = boost
                                flatTrainerPayRate = customRate
                                showTrainerSettingsDialog = false

                                devPrefs.edit()
                                    .putBoolean("is_training_active", active)
                                    .putString("trainee_tier", tier)
                                    .putInt("training_week", week)
                                    .putBoolean("is_tmc_boost_active", boost)
                                    .putString("custom_trainer_rate", customRate)
                                    .apply()
                            },
                            onDismiss = { showTrainerSettingsDialog = false }
                        )
                    }

                    if (showProUpgradeDialog) {
                        ProPaywallDialog(
                            onUnlockClick = {
                                isProUser = true
                                showProUpgradeDialog = false
                                Toast.makeText(this@MainActivity, "🎉 Pro Features Unlocked!", Toast.LENGTH_LONG).show()
                            },
                            onDismiss = { showProUpgradeDialog = false }
                        )
                    }

                    if (showReadmeDialog) {
                        val readmeTextString = remember {
                            try {
                                applicationContext.assets.open("README.md").bufferedReader().use { it.readText() }
                            } catch (_: Exception) {
                                "# Error\nCould not locate `README.md` asset file."
                            }
                        }

                        AlertDialog(
                            onDismissRequest = { showReadmeDialog = false },
                            title = { Text("App Info & Technical Overview") },
                            text = {
                                Box(modifier = Modifier.heightIn(max = 450.dp)) {
                                    LazyColumn {
                                        item {
                                            MarkdownText(
                                                markdown = readmeTextString,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Button(onClick = { showReadmeDialog = false }) {
                                    Text("Close")
                                }
                            }
                        )
                    }

                    if (showDevAccessRequestDialog) {
                        var requesterName by remember { mutableStateOf("") }
                        var requesterEmail by remember { mutableStateOf("") }
                        var requesterNote by remember { mutableStateOf("") }
                        var isSubmitting by remember { mutableStateOf(false) }

                        AlertDialog(
                            onDismissRequest = {
                                if (!isSubmitting) showDevAccessRequestDialog = false
                            },
                            title = { Text("📱 Request Developer Access") },
                            text = {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        "Submit your device information to request developer/tester access to advanced tools.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("Your Device ID:", style = MaterialTheme.typography.labelSmall)
                                            Text(currentDeviceId, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    OutlinedTextField(
                                        value = requesterName,
                                        onValueChange = { requesterName = it },
                                        label = { Text("Your Name (Required)") },
                                        placeholder = { Text("e.g. Driver John") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = requesterEmail,
                                        onValueChange = { requesterEmail = it },
                                        label = { Text("Your Email Address (Required)") },
                                        placeholder = { Text("e.g. john@example.com") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = requesterNote,
                                        onValueChange = { requesterNote = it },
                                        label = { Text("Reason / Note (Optional)") },
                                        placeholder = { Text("e.g. Need access to test SQLite database viewer") },
                                        minLines = 2,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        if (requesterName.isBlank() || requesterEmail.isBlank()) {
                                            Toast.makeText(this@MainActivity, "Please enter your Name and Email.", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        isSubmitting = true
                                        val repo = devPrefs.getString("gh_repo", BuildConfig.DEFAULT_GITHUB_REPO) ?: BuildConfig.DEFAULT_GITHUB_REPO
                                        val token = devPrefs.getString("gh_token", "")?.ifBlank { BuildConfig.DEFAULT_GITHUB_TOKEN } ?: BuildConfig.DEFAULT_GITHUB_TOKEN

                                        scope.launch {
                                            val (success, message) = submitDevAccessRequest(
                                                repo = repo,
                                                token = token,
                                                name = requesterName,
                                                email = requesterEmail,
                                                deviceId = currentDeviceId,
                                                note = requesterNote
                                            )
                                            isSubmitting = false
                                            Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                                            if (success) {
                                                showDevAccessRequestDialog = false
                                            }
                                        }
                                    },
                                    enabled = !isSubmitting && requesterName.isNotBlank() && requesterEmail.isNotBlank()
                                ) {
                                    if (isSubmitting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Submitting...")
                                    } else {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("🚀 Submit Access Request")
                                    }
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { showDevAccessRequestDialog = false },
                                    enabled = !isSubmitting
                                ) { Text("Cancel") }
                            }
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

    private suspend fun submitDevAccessRequest(
        repo: String,
        token: String,
        name: String,
        email: String,
        deviceId: String,
        note: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val cleanRepo = repo.trim().removePrefix("https://github.com/").removeSuffix(".git")
            if (cleanRepo.isBlank() || token.trim().isBlank()) {
                return@withContext Pair(false, "Developer access request service is currently offline.")
            }
            val url = URL("https://api.github.com/repos/$cleanRepo/issues")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer ${token.trim()}")
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("Content-Type", "application/json; utf-8")
            conn.setRequestProperty("User-Agent", "LoadTrackerPro")
            conn.doOutput = true

            val title = "[Dev Access Request]: $name"
            val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy @ hh:mm a")
            val nowFormatted = Instant.ofEpochMilli(System.currentTimeMillis()).atZone(ZoneId.systemDefault()).format(formatter)

            val bodyText = """
                ### 📱 Developer Access Request
                - **Requester Name:** $name
                - **Email Address:** $email
                - **Device ID:** `$deviceId`
                - **Requested On:** $nowFormatted
                ${if (note.isNotBlank()) "- **Notes / Reason:** $note" else ""}
            """.trimIndent()

            val jsonPayload = """
                {
                  "title": "${escapeJsonPayload(title)}",
                  "body": "${escapeJsonPayload(bodyText)}",
                  "labels": ["dev-access-request"]
                }
            """.trimIndent()

            conn.outputStream.use { os ->
                os.write(jsonPayload.toByteArray(Charsets.UTF_8))
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                Pair(true, "Dev access request submitted! You will be notified once approved.")
            } else {
                val errText = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                Pair(false, "Could not submit request ($responseCode): $errText")
            }
        } catch (e: Exception) {
            Pair(false, "Network error: ${e.localizedMessage}")
        }
    }

    private fun escapeJsonPayload(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
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
