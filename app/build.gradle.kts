import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
}

val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties()

tasks.register("incrementVersionBuild") {
    doLast {
        if (versionPropsFile.exists()) {
            versionProps.load(versionPropsFile.inputStream())
            val currentBuild = versionProps.getProperty("VERSION_BUILD", "1").toInt()
            val nextBuild = currentBuild + 1
            versionProps.setProperty("VERSION_BUILD", nextBuild.toString())
            versionProps.store(versionPropsFile.writer(), "Auto-incremented by Gradle build")
            println("🚀 Auto-incremented VERSION_BUILD to $nextBuild in version.properties")
        }
    }
}

tasks.matching { it.name.startsWith("bundle") || it.name.startsWith("assemble") }.configureEach {
    if (name.contains("Release")) {
        dependsOn("incrementVersionBuild")
    }
}

android {
    namespace = "com.example.tmcloadtracker"
    compileSdk = 36

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

    val vMajor = versionProps.getProperty("VERSION_MAJOR", "1").toInt()
    val vMinor = versionProps.getProperty("VERSION_MINOR", "1").toInt()
    val vPatch = versionProps.getProperty("VERSION_PATCH", "0").toInt()
    val vBuild = versionProps.getProperty("VERSION_BUILD", "4").toInt()

    val vName = "$vMajor.$vMinor.$vPatch"
    val vCode = vBuild

    defaultConfig {
        minSdk = 27
        targetSdk = 36
        versionCode = vCode
        versionName = vName

        buildConfigField("String", "DEFAULT_GITHUB_TOKEN", "\"$defaultGithubToken\"")
        buildConfigField("String", "DEFAULT_GITHUB_REPO", "\"$defaultGithubRepo\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "version"

    productFlavors {
        create("v1") {
            dimension = "version"
            applicationId = "com.loadtracker.pro"
            versionName = "1.10.0"
        }
        create("v2") {
            dimension = "version"
            applicationId = "com.loadtracker.pro.v2"
            versionName = "2.0.0"
            resValue("string", "app_name", "Load Tracker v2 🚀")
        }
    }

    signingConfigs {
        create("sharedDebug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            storeFile = file("release.keystore")
            storePassword = "loadtrackerpass"
            keyAlias = "loadtracker_key"
            keyPassword = "loadtrackerpass"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("sharedDebug")
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
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
    kapt(libs.androidx.room.compiler)

    // Services & Utilities
    implementation(libs.play.services.location)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.compose.markdown)
    implementation(libs.coil.compose)

    // 🗺️ Google Maps for Route Visualization
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
}
