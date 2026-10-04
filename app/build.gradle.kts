import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.loadtracker.pro"
    compileSdk = 36

    val versionPropsFile = rootProject.file("version.properties")
    val versionProps = Properties()
    if (versionPropsFile.exists()) {
        versionProps.load(versionPropsFile.inputStream())
    }

    val localPropsFile = rootProject.file("local.properties")
    val localProps = Properties()
    if (localPropsFile.exists()) {
        localProps.load(localPropsFile.inputStream())
    }
    val defaultGithubToken = localProps.getProperty("GITHUB_TOKEN", "")
    val defaultGithubRepo = localProps.getProperty("GITHUB_REPO", "pa2122/load_tracker_pro")
    val mapsApiKey = localProps.getProperty("MAPS_API_KEY", "")

    val vMajor = versionProps.getProperty("VERSION_MAJOR", "1").toInt()
    val vMinor = versionProps.getProperty("VERSION_MINOR", "10").toInt()
    val vPatch = versionProps.getProperty("VERSION_PATCH", "0").toInt()
    val vBuild = versionProps.getProperty("VERSION_BUILD", "100").toInt()

    val vName = "$vMajor.$vMinor.$vPatch"
    // Automated Date/Timestamp Version Code (Format: YYMMDDHH, e.g. 26031514)
    val dateVersionCode = try {
        SimpleDateFormat("yyMMddHH", Locale.US).format(Date()).toInt()
    } catch (_: Exception) {
        vBuild
    }
    val vCode = maxOf(vBuild, dateVersionCode)

    defaultConfig {
        applicationId = "com.loadtracker.pro"
        minSdk = 27
        targetSdk = 36
        versionCode = vCode
        versionName = vName

        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        buildConfigField("String", "DEFAULT_GITHUB_TOKEN", "\"$defaultGithubToken\"")
        buildConfigField("String", "DEFAULT_GITHUB_REPO", "\"$defaultGithubRepo\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val releaseStoreFile = localProps.getProperty("RELEASE_STORE_FILE", "")
    val releaseStorePassword = localProps.getProperty("RELEASE_STORE_PASSWORD", "")
    val releaseKeyAlias = localProps.getProperty("RELEASE_KEY_ALIAS", "")
    val releaseKeyPassword = localProps.getProperty("RELEASE_KEY_PASSWORD", "")

    signingConfigs {
        create("sharedDebug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            if (releaseStoreFile.isNotBlank() && file(releaseStoreFile).exists()) {
                storeFile = file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            } else {
                storeFile = file("debug.keystore")
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("sharedDebug")
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            ndk {
                debugSymbolLevel = "FULL"
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Room Database Components
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Services & Utilities
    implementation(libs.play.services.location)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.compose.markdown)
    implementation(libs.coil.compose)

    // 🗺️ Google Maps & Wearable Sync
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.wearable)
}
