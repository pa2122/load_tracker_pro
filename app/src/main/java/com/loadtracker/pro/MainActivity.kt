package com.loadtracker.pro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.net.TrafficStats
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.ui.res.stringResource
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.firstOrNull
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.NavigationDrawerItem
import androidx.lifecycle.lifecycleScope
import com.loadtracker.pro.ui.theme.LoadTrackerProTheme
import com.loadtracker.pro.CurrentLoad
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
        CrashRecoveryHandler.setupGlobalExceptionHandler(this)

        val recoveryState = CrashRecoveryHandler.checkAndClearCrashRecoveryState(this)
        if (recoveryState.wasRecovered) {
            android.widget.Toast.makeText(this, "⚠️ App recovered after unexpected shutdown. Active load tracking resumed.", android.widget.Toast.LENGTH_LONG).show()
        }

        setContent {
            LoadTrackerProTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    var splashDismissed by rememberSaveable { mutableStateOf(value = false) }
                    var settingsRead by remember { mutableStateOf(value = false) }
                    var activeLoadRestored by remember { mutableStateOf(value = false) }
                    var splashTimedOut by remember { mutableStateOf(value = false) }

                    val appVersionName = remember {
                        try {
                            @Suppress("DEPRECATION")
                            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
                        } catch (_: Exception) {
                            "1.0.0"
                        }
                    }

                    var currentScreen by rememberSaveable { mutableStateOf(value = "dashboard") }
                    val loadByProSaver = Saver<CurrentLoad?, String>(
                        save = { it?.proNumber.orEmpty() },
                        restore = { pro -> viewModel.allLoads.value.find { it.proNumber == pro } }
                    )
                    var tripToEdit by rememberSaveable(stateSaver = loadByProSaver) {
                        mutableStateOf<CurrentLoad?>(value = null)
                    }
                    var tripForMap by rememberSaveable(stateSaver = loadByProSaver) {
                        mutableStateOf<CurrentLoad?>(value = null)
                    }

                    val backupRestoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                        if (uri != null) {
                            val (success, msg) = DatabaseBackupManager.restoreDatabase(this@MainActivity, uri)
                            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                        }
                    }

                    var showConfigureRatesDialog by remember { mutableStateOf(value = false) }
                    var showBackupRestoreDialog by remember { mutableStateOf(value = false) }
                    var showReplaceBackupPrompt by remember { mutableStateOf(value = false) }
                    var showPostBackupShareDialog by remember { mutableStateOf(value = false) }
                    var createdBackupFileForShare by remember { mutableStateOf<File?>(value = null) }

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
                    var isResolvingHomeAddress by remember { mutableStateOf(value = false) }
                    val scope = rememberCoroutineScope()

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
                    var isHistoricalEntry by rememberSaveable { mutableStateOf(value = false) }

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

                    var locallyAuthorizedDevices by remember {
                        mutableStateOf(
                            (devPrefs.getStringSet("authorized_devices", emptySet()) ?: emptySet()).toSet()
                        )
                    }
                    var remoteAuthorizedDevices by remember {
                        mutableStateOf(
                            (devPrefs.getStringSet("remote_authorized_devices", emptySet()) ?: emptySet()).toSet()
                        )
                    }
                    val isTesterAllowlisted = BuildConfig.DEBUG ||
                            currentDeviceId in remoteAuthorizedDevices
                    val isDeveloperAuthorized = DeveloperAccessResolver.hasAccess(
                        isDebugBuild = BuildConfig.DEBUG,
                        isDeveloperDeviceAllowlisted = currentDeviceId in locallyAuthorizedDevices
                    )
                    val isProUser = ProEntitlementResolver.resolve(
                        hasVerifiedPurchase = false,
                        isTesterAllowlisted = isTesterAllowlisted
                    ).hasProAccess
                    var newTesterDeviceId by remember { mutableStateOf(value = "") }

                    fun resolveHomeAddress(input: String, showErrors: Boolean = false) {
                        val query = HomeAddressResolver.createQuery(input) ?: return
                        scope.launch {
                            isResolvingHomeAddress = true
                            try {
                                val location = HomeAddressResolver.resolve(this@MainActivity, query)
                                if (homeRawPaste == input && location != null) {
                                    homeLat = location.latitude
                                    homeLong = location.longitude
                                    isHomeVerified = true
                                    location.displayName?.let { homeBase = it }
                                } else if (showErrors && homeRawPaste == input) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Address could not be verified.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("MainActivity", "Unable to resolve home address", e)
                                if (showErrors && homeRawPaste == input) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Address lookup failed. Check your connection and try again.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } finally {
                                isResolvingHomeAddress = false
                            }
                        }
                    }

                    LaunchedEffect(isProUser) {
                        TrackingService.isProUser = isProUser
                    }

                    LaunchedEffect(Unit) {
                        try {
                            val remoteIds = TesterAllowlistRepository.fetch()
                            remoteAuthorizedDevices = remoteIds
                            devPrefs.edit()
                                .putStringSet("remote_authorized_devices", remoteIds)
                                .apply()
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            android.util.Log.w(
                                "MainActivity",
                                "Unable to refresh tester allowlist; using cached access",
                                e
                            )
                        }
                    }

                    LaunchedEffect(Unit) {
                        withContext(Dispatchers.IO) {
                            resolveHomeAddress("15381 TX-198, Mabank, TX 75147")
                        }
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
                    val savedLoads by viewModel.allLoads.collectAsState(initial = emptyList())
                    val loadsLoaded by viewModel.loadsLoaded.collectAsState()
                    val hasUnfinishedLoad = loadsLoaded &&
                        viewModel.allLoads.value.any { StartupSplashLogic.isUnfinished(it.tripState) }
                    val isAppStartingUp = StartupSplashLogic.shouldShowSplash(
                        settingsRead = settingsRead,
                        loadsLoaded = loadsLoaded,
                        hasUnfinishedLoad = hasUnfinishedLoad,
                        activeLoadRestored = activeLoadRestored,
                        alreadyDismissed = splashDismissed
                    )
                    val splashStep = StartupSplashLogic.currentStep(
                        settingsRead = settingsRead,
                        loadsLoaded = loadsLoaded,
                        hasUnfinishedLoad = hasUnfinishedLoad,
                        activeLoadRestored = activeLoadRestored,
                        recoveredAfterShutdown = recoveryState.wasRecovered
                    )

                    LaunchedEffect(Unit) {
                        withContext(Dispatchers.IO) {
                            getSharedPreferences("dev_prefs", MODE_PRIVATE).all
                        }
                        settingsRead = true
                    }

                    LaunchedEffect(isAppStartingUp) {
                        if (!isAppStartingUp) {
                            splashDismissed = true
                        } else {
                            delay(StartupSplashLogic.TIMEOUT_MS.milliseconds)
                            splashTimedOut = true
                        }
                    }

                    val currentPayrollWeekStart = remember {
                        getPayrollWeekStartDate(System.currentTimeMillis())
                    }

                    val currentWeekHasTrainingLoad = remember(savedLoads, currentPayrollWeekStart) {
                        savedLoads.any { load ->
                            (load.payrollWeekStartDate() == currentPayrollWeekStart) && load.isTrainingWeek
                        }
                    }

                    LaunchedEffect(key1 = currentWeekHasTrainingLoad) {
                        if (currentWeekHasTrainingLoad) {
                            isTrainingActive = true
                            savedIsTrainingActive = true
                        }
                    }

                    LaunchedEffect(key1 = savedLoads, key2 = loadsLoaded) {
                        val active = savedLoads.find { 
                            (it.tripState != "COMPLETED") && 
                            (it.tripState != "NOT_STARTED") && 
                            (it.tripState != "PAUSED_AT_HOME") 
                        }
                        if (active != null) {
                            TrackingService.activeTripState = active.tripState
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
                                    val nextStop = active.nextLoadedDestination()
                                    TrackingService.targetLat = nextStop?.latitude ?: active.consigneeLat
                                    TrackingService.targetLong = nextStop?.longitude ?: active.consigneeLong
                                    TrackingService.targetName =
                                        nextStop?.name ?: active.consigneeName ?: "Consignee"
                                    TrackingService.isGeofenceActive =
                                        TrackingService.targetLat != null &&
                                                TrackingService.targetLong != null
                                }

                                ContextCompat.startForegroundService(
                                    this@MainActivity,
                                    Intent(this@MainActivity, TrackingService::class.java)
                                )
                            }
                        }
                        if (loadsLoaded && (active != null || !hasUnfinishedLoad)) {
                            activeLoadRestored = true
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
                        extraStopPay,
                        isTrainingActive,
                        calculatedTrainerPay,
                    ) {
                        derivedStateOf {
                            viewModel.getCurrentWeekSummary(
                                lumberRate = tarp8Pay.toDoubleOrNull() ?: 0.0,
                                steelRate = tarp4Pay.toDoubleOrNull() ?: 0.0,
                                extraStopRate = extraStopPay.toDoubleOrNull() ?: 0.0,
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
                                        .padding(all = 16.dp)
                                        .fillMaxHeight()
                                        .verticalScroll(state = drawerScrollState),
                                    verticalArrangement = Arrangement.spacedBy(space = 10.dp)
                                ) {
                                    // 1. Driver Profile Header Card
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Image(
                                                    painter = painterResource(id = R.drawable.app_logo),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                                Text(
                                                    "Load Tracker Pro",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                            Text(
                                                "Active Fleet Driver • v$appVersionName",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    // Section 1: 🚚 NAVIGATION & WORKSPACES
                                    Text("🚚 NAVIGATION & WORKSPACES", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

                                    NavigationDrawerItem(
                                        label = { Text("🏠 Dashboard") },
                                        selected = currentScreen == "dashboard",
                                        onClick = {
                                            scope.launch { drawerState.close() }
                                            currentScreen = "dashboard"
                                        }
                                    )

                                    NavigationDrawerItem(
                                        label = { Text("➕ Add Historical Completed Load") },
                                        selected = currentScreen == "entry" && isHistoricalEntry,
                                        onClick = {
                                            scope.launch { drawerState.close() }
                                            tripToEdit = null
                                            isHistoricalEntry = true
                                            currentScreen = "entry"
                                        }
                                    )

                                    NavigationDrawerItem(
                                        label = { Text("🏢 Review Facility Insights") },
                                        selected = currentScreen == "facility_search",
                                        onClick = {
                                            scope.launch { drawerState.close() }
                                            if (isProUser) currentScreen = "facility_search" else showProUpgradeDialog = true
                                        }
                                    )

                                    NavigationDrawerItem(
                                        label = { Text("🗺️ Global Route Heatmap") },
                                        selected = currentScreen == "route_map",
                                        onClick = {
                                            scope.launch { drawerState.close() }
                                            if (isProUser) { tripForMap = null; currentScreen = "route_map" } else showProUpgradeDialog = true
                                        }
                                    )

                                    HorizontalDivider()

                                    // Section 2: ⚙️ DRIVER SETTINGS
                                    Text("⚙️ DRIVER SETTINGS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

                                    NavigationDrawerItem(
                                        label = { Text("⚙️ Configure Pay Rates & Home") },
                                        selected = false,
                                        onClick = {
                                            scope.launch { drawerState.close() }
                                            showConfigureRatesDialog = true
                                        }
                                    )

                                    NavigationDrawerItem(
                                        label = { Text("🎓 Configure Trainer Pay") },
                                        selected = false,
                                        onClick = {
                                            scope.launch { drawerState.close() }
                                            showTrainerSettingsDialog = true
                                        }
                                    )

                                    HorizontalDivider()

                                    // Section 3: 💾 DATA, UTILITIES & DEV
                                    Text("💾 DATA & UTILITIES", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

                                    NavigationDrawerItem(
                                        label = { Text("📄 Export Settlement Log (CSV)") },
                                        selected = false,
                                        onClick = {
                                            scope.launch { drawerState.close() }
                                            exportToCsv()
                                        }
                                    )

                                    NavigationDrawerItem(
                                        label = { Text("💾 Export Data") },
                                        selected = false,
                                        onClick = {
                                            scope.launch { drawerState.close() }
                                            showBackupRestoreDialog = true
                                        }
                                    )

                                    NavigationDrawerItem(
                                        label = { Text("🐛 Report Bug / Feedback") },
                                        selected = false,
                                        onClick = {
                                            scope.launch { drawerState.close() }
                                            showTesterFeedbackDialog = true
                                        }
                                    )

                                    NavigationDrawerItem(
                                        label = { Text("📖 App Technical Specs (README)") },
                                        selected = false,
                                        onClick = {
                                            scope.launch { drawerState.close() }
                                            showReadmeDialog = true
                                        }
                                    )

                                    if (isDeveloperAuthorized) {
                                        NavigationDrawerItem(
                                            label = { Text("📥 Screenshot Load Import") },
                                            selected = currentScreen == "batch_import",
                                            onClick = {
                                                scope.launch { drawerState.close() }
                                                currentScreen = "batch_import"
                                            }
                                        )
                                        NavigationDrawerItem(
                                            label = { Text("🛠️ Developer Options & Notes") },
                                            selected = false,
                                            onClick = {
                                                scope.launch { drawerState.close() }
                                                showDevOptionsDialog = true
                                            }
                                        )
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
                        run {
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
                                                if (isProUser) {
                                                    tripForMap = null
                                                    currentScreen = "route_map"
                                                } else {
                                                    showProUpgradeDialog = true
                                                }
                                            },
                                            icon = { Icon(Icons.Default.LocationOn, contentDescription = "Heatmap") },
                                            label = { Text("Heatmap") }
                                        )
                                        NavigationRailItem(
                                            selected = currentScreen == "facility_search",
                                            onClick = {
                                                if (isProUser) {
                                                    currentScreen = "facility_search"
                                                } else {
                                                    showProUpgradeDialog = true
                                                }
                                            },
                                            icon = { Icon(Icons.Default.Search, contentDescription = "Facilities") },
                                            label = { Text("Facilities") }
                                        )
                                        NavigationRailItem(
                                            selected = false,
                                            onClick = { showTesterFeedbackDialog = true },
                                            icon = { Icon(Icons.Default.Edit, contentDescription = "Feedback") },
                                            label = { Text("Feedback") }
                                        )
                                        if (isDeveloperAuthorized) {
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
                                                text = if (splashTimedOut) {
                                                    stringResource(R.string.splash_timeout_error)
                                                } else {
                                                    stringResource(
                                                        when (splashStep) {
                                                            StartupStep.READING_SETTINGS -> R.string.splash_reading_settings
                                                            StartupStep.OPENING_DATABASE -> R.string.splash_opening_database
                                                            StartupStep.RESTORING_ACTIVE_LOAD -> R.string.splash_restoring_active_load
                                                            StartupStep.RECOVERED_AFTER_SHUTDOWN -> R.string.splash_recovered_after_shutdown
                                                        }
                                                    )
                                                },
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (splashTimedOut) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                                            )

                                            if (splashTimedOut) {
                                                Button(onClick = { splashDismissed = true }) {
                                                    Text(stringResource(R.string.splash_continue_anyway))
                                                }
                                            }
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
                                                    isHistoricalEntry = false
                                                    currentScreen = "entry" 
                                                },
                                                onAddHistoricalLoadClick = {
                                                    tripToEdit = null
                                                    isHistoricalEntry = true
                                                    currentScreen = "entry"
                                                },
                                                onEditTripClick = { load ->
                                                    tripToEdit = load
                                                    currentScreen = "entry"
                                                },
                                                onDeleteTripClick = { load ->
                                                    if (load.tripState != "COMPLETED") {
                                                        stopService(Intent(this@MainActivity, TrackingService::class.java))
                                                        TrackingService.resetTrackingState(this@MainActivity)
                                                    }
                                                    viewModel.deleteLoad(load)
                                                },
                                                liveBounceMiles = liveBounce,
                                                liveLoadedMiles = liveLoaded,
                                                isTrainingActive = isTrainingActive,
                                                flatTrainerPayRate = calculatedTrainerPay,
                                                extraStopRate = extraStopPay.toDoubleOrNull() ?: 0.0,
                                                showHelpOnLaunch = triggerHelpView.value,
                                                onDismissHelpDialog = {
                                                    triggerHelpView.value = false
                                                },
                                                onViewMapClick = { load ->
                                                    tripForMap = load
                                                    currentScreen = "route_map"
                                                },
                                                onEditDeliveryDate = { load, ts ->
                                                    viewModel.updateDeliveryTimestamp(load, ts)
                                                },
                                                onUpdateTripClick = { updatedTripEntity ->
                                                    if (updatedTripEntity.tripState == "COMPLETED") {
                                                        stopService(
                                                            Intent(
                                                                this@MainActivity,
                                                                TrackingService::class.java
                                                            )
                                                        )
                                                        TrackingService.resetTrackingState(this@MainActivity)
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
                                                        detentionFlatPay = updatedTripEntity.detentionFlatPay,
                                                        extraStopCount = updatedTripEntity.extraStopCount,
                                                        stopOffsData = updatedTripEntity.stopOffsData,
                                                        currentStopIndex = updatedTripEntity.currentStopIndex,
                                                        lastStopLoadedMiles = updatedTripEntity.lastStopLoadedMiles
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
                                                lookupFacilityNote = { name -> viewModel.getFacilityNote(name) },
                                                saveFacilityNote = { note -> viewModel.saveFacilityNote(note) },
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
                                                        deliveryTimestamp = finalizedLoadEntity.deliveryTimestamp,
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
                                                        detentionFlatPay = finalizedLoadEntity.detentionFlatPay,
                                                        extraStopCount = finalizedLoadEntity.extraStopCount,
                                                        stopOffsData = finalizedLoadEntity.stopOffsData,
                                                        currentStopIndex = finalizedLoadEntity.currentStopIndex,
                                                        lastStopLoadedMiles = finalizedLoadEntity.lastStopLoadedMiles
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

                                        "batch_import" -> {
                                            if (isDeveloperAuthorized) {
                                                ScreenshotBatchImportScreen(
                                                    existingProNumbers = savedLoads.map { it.proNumber }.toSet(),
                                                    defaultPercentage = defPercent,
                                                    onBack = { currentScreen = "dashboard" }
                                                )
                                            } else {
                                                currentScreen = "dashboard"
                                            }
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
                                        Text("Pro access", style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            if (isProUser) "Granted" else "Not granted",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isProUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
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

                                    Text("Developer Device Authorization", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "Device ID: $currentDeviceId",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )

                                    Button(
                                        onClick = {
                                            val currentSet = locallyAuthorizedDevices.toMutableSet()
                                            if (currentDeviceId in currentSet) {
                                                currentSet.remove(currentDeviceId)
                                                Toast.makeText(this@MainActivity, "Developer access removed", Toast.LENGTH_SHORT).show()
                                            } else {
                                                currentSet.add(currentDeviceId)
                                                Toast.makeText(this@MainActivity, "Developer access authorized for this device", Toast.LENGTH_SHORT).show()
                                            }
                                            locallyAuthorizedDevices = currentSet.toSet()
                                            devPrefs.edit().putStringSet("authorized_devices", currentSet).apply()
                                        },
                                        enabled = !BuildConfig.DEBUG,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isDeveloperAuthorized) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = if (isDeveloperAuthorized) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            when {
                                                BuildConfig.DEBUG -> "✅ Developer access enabled in debug build"
                                                currentDeviceId in locallyAuthorizedDevices -> "✅ Developer access enabled (Tap to revoke)"
                                                else -> "📱 Enable Developer Options on this device"
                                            }
                                        )
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
                                                locallyAuthorizedDevices = currentSet.toSet()
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

                    if (showConfigureRatesDialog) {
                        AlertDialog(
                            onDismissRequest = { showConfigureRatesDialog = false },
                            title = { Text("⚙️ Configure Pay Rates & Home Base") },
                            text = {
                                val ratesScrollState = rememberScrollState()
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth().verticalScroll(ratesScrollState)
                                ) {
                                    OutlinedTextField(
                                        value = defPercent,
                                        onValueChange = { input ->
                                            val filtered = input.filter { it.isDigit() || it == '.' }
                                            if (filtered.isEmpty() || filtered == ".") defPercent = filtered
                                            else if (filtered.count { it == '.' } <= 1) {
                                                val value = filtered.toDoubleOrNull()
                                                if ((value != null) && (value >= 0.0) && (value <= 100.0)) defPercent = filtered
                                            }
                                        },
                                        label = { Text("Default Pay Rate (%)") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = tarp8Pay,
                                        onValueChange = { input -> tarp8Pay = input.filter { it.isDigit() || it == '.' } },
                                        label = { Text("8' Drop Tarp Pay ($)") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = tarp4Pay,
                                        onValueChange = { input -> tarp4Pay = input.filter { it.isDigit() || it == '.' } },
                                        label = { Text("4' Drop Tarp Pay ($)") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = extraStopPay,
                                        onValueChange = { input -> extraStopPay = input.filter { it.isDigit() || it == '.' } },
                                        label = { Text("Extra Stop Pay per Stop Off ($)") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(
                                        "This rate is added once for each Stop Off recorded on a load.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
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
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { resolveHomeAddress(homeRawPaste, showErrors = true) },
                                            enabled = homeRawPaste.isNotBlank() && !isHomeVerified && !isResolvingHomeAddress,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                when {
                                                    isResolvingHomeAddress -> "Verifying..."
                                                    isHomeVerified -> "Verified ✅"
                                                    else -> "Verify Address"
                                                }
                                            )
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
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.LocationOn, contentDescription = null)
                                            Spacer(Modifier.width(4.dp))
                                            Text("Pin Current")
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        savedDefPercent = defPercent
                                        savedTarp8Pay = tarp8Pay
                                        savedTarp4Pay = tarp4Pay
                                        savedExtraStopPay = extraStopPay
                                        savedHomeRawPaste = homeRawPaste
                                        devPrefs.edit().apply {
                                            putString("def_percent", defPercent)
                                            putString("tarp_8_pay", tarp8Pay)
                                            putString("tarp_4_pay", tarp4Pay)
                                            putString("extra_stop_pay", extraStopPay)
                                            putString("home_raw_paste", homeRawPaste)
                                            apply()
                                        }
                                        resolveHomeAddress(homeRawPaste, showErrors = true)
                                        showConfigureRatesDialog = false
                                    }
                                ) {
                                    Text("Save Changes")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showConfigureRatesDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    if (showBackupRestoreDialog) {
                        var backupRefreshKey by remember { mutableIntStateOf(0) }
                        val backupFiles = remember(backupRefreshKey) { DatabaseBackupManager.getAllBackupFiles(this@MainActivity) }
                        val latestBackupFile = DatabaseBackupManager.getLatestBackupFile(this@MainActivity)
                        val backupMeta = remember(latestBackupFile) { DatabaseBackupManager.getBackupMetadata(latestBackupFile) }
                        val latestAutomaticBackup = DatabaseBackupManager.getBackupMetadata(
                            DatabaseBackupManager.getLatestAutomaticBackupFile(this@MainActivity)
                        )
                        val automaticBackupError = getSharedPreferences("app_settings", MODE_PRIVATE)
                            .getString(DatabaseBackupManager.AUTO_BACKUP_ERROR_KEY, null)

                        val executeBackup: (replace: Boolean) -> Unit = { replace ->
                            val (backupFile, msg) = DatabaseBackupManager.createDatabaseBackup(this@MainActivity, replacePrevious = replace)
                            if (backupFile != null) {
                                backupRefreshKey++
                                createdBackupFileForShare = backupFile
                                showPostBackupShareDialog = true
                            } else {
                                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                            }
                        }

                        AlertDialog(
                            onDismissRequest = { showBackupRestoreDialog = false },
                            title = { Text("💾 Export & Restore Data") },
                            text = {
                                val backupScrollState = rememberScrollState()
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth().verticalScroll(backupScrollState)
                                ) {
                                    Text(
                                        "Safely export a timestamped backup copy of your trip logs, rate confirmations, fuel records, and settings.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        latestAutomaticBackup?.let {
                                            "Automatic on-device snapshot: ${it.formattedDate}. Up to 14 daily snapshots are retained; they stay on this phone."
                                        } ?: "Automatic on-device snapshot: none yet. A snapshot is created before the first database change each day.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    automaticBackupError?.let {
                                        Text(
                                            "The latest automatic snapshot attempt failed: $it",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            if (backupMeta != null) {
                                                showReplaceBackupPrompt = true
                                            } else {
                                                executeBackup(false)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("📤 Create Data Backup")
                                    }

                                    Button(
                                        onClick = {
                                            showBackupRestoreDialog = false
                                            backupRestoreLauncher.launch("*/*")
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("📥 Import & Restore External File")
                                    }

                                    HorizontalDivider()

                                    // Local Data Backup History List
                                    if (backupFiles.isEmpty()) {
                                        Text(
                                            "📍 No local data backups found.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Text(
                                            "📦 Local Data Backups Saved (${backupFiles.size} Files):",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            backupFiles.forEach { bFile ->
                                                val meta = DatabaseBackupManager.getBackupMetadata(bFile)
                                                if (meta != null) {
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.padding(10.dp),
                                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Text("• ${meta.fileName}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                                            Text("  Date: ${meta.formattedDate}", style = MaterialTheme.typography.labelSmall)
                                                            Text("  Size: ${meta.sizeKb} KB • Validated ✅", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)

                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.End,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Button(
                                                                    onClick = {
                                                                        val (success, msg) = DatabaseBackupManager.restoreDatabaseFromFile(this@MainActivity, bFile)
                                                                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                                                                        if (success) showBackupRestoreDialog = false
                                                                    },
                                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                                    modifier = Modifier.height(28.dp),
                                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                                ) {
                                                                    Text("Restore", fontSize = 10.sp)
                                                                }

                                                                Spacer(modifier = Modifier.width(6.dp))

                                                                IconButton(
                                                                    onClick = {
                                                                        bFile.delete()
                                                                        backupRefreshKey++
                                                                        Toast.makeText(this@MainActivity, "Deleted backup: ${bFile.name}", Toast.LENGTH_SHORT).show()
                                                                    },
                                                                    modifier = Modifier.size(28.dp)
                                                                ) {
                                                                    Icon(Icons.Default.Delete, contentDescription = "Delete Backup", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showBackupRestoreDialog = false }) {
                                    Text("Close")
                                }
                            }
                        )

                        if (showReplaceBackupPrompt) {
                            AlertDialog(
                                onDismissRequest = { showReplaceBackupPrompt = false },
                                title = { Text("⚠️ Replace Existing Backup?") },
                                text = {
                                    Text(
                                        "An existing data backup file was found on this device (${backupMeta?.fileName}). Would you like to replace the previous backup or keep both as separate timestamped files?",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            showReplaceBackupPrompt = false
                                            executeBackup(true)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("Replace Previous")
                                    }
                                },
                                dismissButton = {
                                    Button(
                                        onClick = {
                                            showReplaceBackupPrompt = false
                                            executeBackup(false)
                                        }
                                    ) {
                                        Text("Keep Both (New File)")
                                    }
                                }
                            )
                        }

                        if (showPostBackupShareDialog && createdBackupFileForShare != null) {
                            val fileToShare = createdBackupFileForShare!!
                            AlertDialog(
                                onDismissRequest = { showPostBackupShareDialog = false },
                                title = { Text("✓ Local Backup Created Successfully!") },
                                text = {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("• File: ${fileToShare.name}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        Text("• Location: ${fileToShare.absolutePath}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Would you like to export or save a copy to Google Drive, Documents, or Email as well?",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            showPostBackupShareDialog = false
                                            val shareUri = DatabaseBackupManager.getShareableUri(this@MainActivity, fileToShare)
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "application/x-sqlite3"
                                                putExtra(Intent.EXTRA_SUBJECT, "Load Tracker Pro Data Backup")
                                                putExtra(Intent.EXTRA_STREAM, shareUri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            startActivity(Intent.createChooser(intent, "Share / Save Backup File"))
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("☁️ Export / Share File")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showPostBackupShareDialog = false }) {
                                        Text("Done (Keep Local Only)")
                                    }
                                }
                            )
                        }
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
                                showProUpgradeDialog = false
                                Toast.makeText(
                                    this@MainActivity,
                                    "Google Play billing is not available yet.",
                                    Toast.LENGTH_LONG
                                ).show()
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
                                        val tokenResult = GitHubTokenStore.load(this@MainActivity)
                                        val token = tokenResult.token

                                        scope.launch {
                                            if (tokenResult.error != null) {
                                                isSubmitting = false
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    tokenResult.error,
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                return@launch
                                            }
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
        val extraStopRate = getSharedPreferences("dev_prefs", MODE_PRIVATE)
            .getString("extra_stop_pay", "0.0")
            ?.toDoubleOrNull() ?: 0.0
        val csvContent = viewModel.generateCsvContent(extraStopRate)
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
