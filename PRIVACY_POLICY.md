# 🔒 Privacy Policy for Load Tracker Pro

**Effective Date**: March 15, 2026  
**Last Updated**: March 15, 2026  
**App Name**: Load Tracker Pro (`com.loadtracker.pro`)  
**Developer**: pa2122  

---

## 1. Overview
**Load Tracker Pro** is committed to protecting your privacy and handling your data with complete transparency. This application is designed specifically for flatbed truck drivers and professional haulers to automate trip auditing, real-time GPS mileage tracking, rate confirmation OCR scanning, and payroll calculations.

---

## 2. Information We Collect and How It Is Used

### 📍 Location Data (Foreground & Background Location)
* **Purpose**: Load Tracker Pro collects precise GPS location data (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`) to automatically measure actual miles driven (segmenting deadhead/bounce miles vs. loaded miles), trigger geofenced arrival notifications at shippers and consignees, and render trip breadcrumb route maps.
* **Background Tracking**: Location tracking operates as an Android Foreground Service with a persistent notification HUD while a trip is active. Background location access is used strictly to maintain accurate odometer distance measurements while the app is minimized or the screen is off.
* **Privacy Guarantee**: Your GPS location data is processed locally on your device hardware and is **never** sold, rented, or shared with third-party advertisers or data brokers.

### 📷 Camera & Photo Media Access
* **Purpose**: Load Tracker Pro accesses your camera and gallery media strictly when you choose to scan rate confirmation screenshots or fuel receipts using Google ML Kit Optical Character Recognition (OCR).
* **Processing**: Image text recognition is performed **100% offline on your device**. Images and parsed dispatch details are stored locally on your device storage.

### 💾 Local Storage & Database Persistence
* **Purpose**: Trip logs, mileage records, fuel entries, and configuration settings are saved locally on your device in an encrypted SQLite database (`Room`).
* **Data Control**: You have full control over your data. Deleting a record inside the app permanently removes it from your device storage.

---

## 3. Third-Party Services
Load Tracker Pro utilizes select trusted Google Play Services for core app functionality:
* **Google Play Services Location & Maps API**: For location updates and map rendering.
* **Google ML Kit Text Recognition**: For offline, on-device optical character recognition.

---

## 4. Data Security
We implement industry-standard security measures, including local device sandbox isolation and build-time API token injection, ensuring that your financial rates, load notes, and location history remain protected on your personal hardware.

---

## 5. Children's Privacy
Load Tracker Pro is intended for professional truck drivers and commercial haulers. The app does not knowingly collect personal information from children under the age of 13.

---

## 6. Contact Us
If you have questions, feedback, or concerns regarding this Privacy Policy, please contact us:
* **GitHub Repository**: [https://github.com/pa2122/load_tracker_pro](https://github.com/pa2122/load_tracker_pro)
* **Developer Issues**: [https://github.com/pa2122/load_tracker_pro/issues](https://github.com/pa2122/load_tracker_pro/issues)
