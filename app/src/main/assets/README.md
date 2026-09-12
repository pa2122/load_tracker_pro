# 🚚 Load Tracker Pro

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin_2.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack_Compose_M3-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Version](https://img.shields.io/badge/Version-v1.2.0-F59E0B)](https://github.com/pa2122/android_apps)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**Load Tracker Pro** is an industrial-grade, native Android application engineered specifically for flatbed truck drivers and professional haulers. It automates trip auditing, GPS mileage tracking, rate confirmation OCR scanning, and weekly payroll calculations with industrial precision.

---

## 🌟 Key Features

### 📷 OCR Dispatch Screenshot Auto-Fill (Pro)
* **1-Tap Scan**: Select a screenshot of your rate confirmation, dispatch email, or load board app.
* **On-Device ML Kit**: Uses Google ML Kit Text Recognition to scan images offline in under half a second.
* **Smart Auto-Population**: Automatically extracts and populates:
  * **PRO / Order Number**
  * **Truck Gross Pay / Revenue**
  * **Dispatched Bounce (Deadhead) & Loaded Miles**
  * **Shipper & Consignee Names / Addresses** (automatically geocoded into exact GPS coordinates)

### 🛰️ Real-Time GPS Tracking & Notification HUD
* **Automatic Background Odometer**: Measures actual miles driven using high-accuracy GPS.
* **Segment Auditing**: Automatically separates **Bounce (Deadhead)** and **Loaded** miles.
* **Notification Shade HUD**: Monitor live mileage, current trip state, and geofence status directly from your phone's notification shade.
* **Geofence Arrival Detection**: 1,000-foot virtual fence around shippers and consignees that alerts you and switches trip state upon arrival.

### 💰 Flatbed Payroll Engine & Statements
* **Friday-to-Thursday Payroll Cycle**: Automatically groups loads picked up Friday through Thursday into the pay week ending on Friday.
* **Itemized Statement Summary**: View comprehensive weekly payroll statements detailing:
  * **Total Truck Gross Revenue**
  * **Driver Load Cut (%)**
  * **Tarping Pay** (Configurable 8' Drop & 4' Drop rates, with automatic pre-tarped halving)
  * **Deadhead Pay** ($0.20/mile bonus for dispatched deadhead $\ge$ 150 miles)
  * **Trainer Pay** ($200.00 flat weekly trainer premium)
  * **Total Estimated Net Take-Home Pay**
* **Current Week Dashboard**: Main screen displays completed loads for the active pay week (ordered newest first), with past weeks safely archived under **View Statements by Week**.

### 🗺️ Advanced Route Mapping & Heatmaps (Pro)
* **Individual Trip Maps**: View exact GPS paths, pickup markers, delivery points, and home base pins.
* **Global Lane Heatmap**: View every mile you've ever tracked on a single national map.
* **Gesture Lock**: Drawer swipe gestures are disabled on map screens to allow smooth 360° map panning and zooming.
* **Home Base Pinning**: Pin your custom home address (`15381 TX-198, Mabank, TX 75147` default) for visual reference and safe zone tracking.

### 🏢 Facility Insights (Pro)
* **Searchable Facility Directory**: Look up shippers and receivers by name to review historical notes.
* **Driver Notes Archive**: Remember gate codes, tight turns, loading procedures, and contact details from past visits.
* **Easy Back Navigation**: Dedicated `← All Facilities` button and back-gesture intercept for smooth navigation.

### 🛠️ Developer Options & GitHub Issues Integration
* **Local Scratchpad (`dev_notes.txt`)**: Full-screen developer notes pad to jot down bugs and feature ideas, saved locally on your device.
* **GitHub Issues API Sync**: Push bugs and feature requests directly to your GitHub repository (`pa2122/android_apps`) as official GitHub Issues.
* **Read-Only GitHub Issue Reader**: Browse, filter (Open, Closed, All), and read repository issues directly inside the app.
* **Connection Tester**: Live connection status indicator (🟢 Connected / 🔴 Failed) with a 1-tap test button.

---

## 🏗️ Technical Architecture & Stack

* **Language**: Kotlin 2.0 with Jetpack Compose Material 3 UI toolkit.
* **Database**: Room SQLite Database (Coroutines Flow & Suspend support).
* **Machine Learning**: Google ML Kit Text Recognition (On-Device, 100% Offline).
* **Location & Maps**: Google Play Services Fused Location Client & Google Maps Compose SDK.
* **Version Management**: Centralized Gradle Version Catalog (`gradle/libs.versions.toml`) + dynamic `version.properties` auto-versioning.
* **Security**: `local.properties` token injection (`BuildConfig.DEFAULT_GITHUB_TOKEN`) ensuring API keys are never exposed in Git control.

---

## 🚀 Getting Started

### Prerequisites
* **Android Studio** Ladybug (2024.2.1) or newer
* **Android SDK**: API Level 35 (Compile SDK 35, Min SDK 27)
* **JDK**: Java 11 / 17

### Building from Source

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/pa2122/android_apps.git
   cd android_apps
   ```

2. **Configure Local Properties** *(Optional for GitHub Sync)*:
   Create or open `local.properties` in the project root and add your GitHub token:
   ```properties
   GITHUB_TOKEN=github_pat_...
   ```

3. **Build & Run**:
   Open the project in Android Studio, select the `:app` module, and click **Run (Shift + F10)**.

---

## 📋 Version History & Release Policy

We follow strict **Semantic Versioning (`MAJOR.MINOR.PATCH`)**:

| Version | Build Code | Release Notes |
| :--- | :--- | :--- |
| **`v1.2.0`** | `9` | Added Read-Only GitHub Issue Reader with filter chips & auto-refresh |
| **`v1.1.4`** | `8` | Improved address verification state tracking (disable on verified, reset on edit) |
| **`v1.1.3`** | `7` | Auto-sync Active Training Week state for active pay period loads |
| **`v1.1.1`** | `5` | Refined drawer layout with top-right Checkmark Save icon & bottom Dev Options button |
| **`v1.1.0`** | `4` | Introduced Developer Options, Dev Notes Scratchpad, and GitHub Issue Sync |
| **`v1.0.2`** | `3` | Designed custom Steel & Cobalt app launcher icon & branded startup screen |
| **`v1.0.0`** | `1` | Initial baseline release (GPS tracking, flatbed payroll engine, heatmaps) |

---

## 📜 License

Distributed under the MIT License. See `LICENSE` for more information.
