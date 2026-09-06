# Load Tracker Pro - Driver's User Manual

Welcome to **Load Tracker Pro**, your professional flatbed hauling companion. This manual provides step-by-step instructions on how to use every feature in the app to track your mileage, audit your payroll, and manage your freight loads with precision.

---

## 🚀 Quick Start: Logging Your First Trip

Follow these simple steps to log and track a new freight load:

1. **Tap `Create New Load Entry`**:
   - Open Load Tracker Pro and tap the **Create New Load Entry** button on the main dashboard.
2. **Enter or Scan Load Details**:
   - Enter your **PRO #**, **Dispatched Bounce (Deadhead) Miles**, **Dispatched Loaded Miles**, and **Truck Gross Pay**.
   - Or tap **📷 Auto-Fill from Screenshot (Pro)** to instantly scan a rate confirmation photo!
3. **Paste Shipper & Consignee Info**:
   - Paste the Shipper name and address, then tap **Verify**.
   - Paste the Consignee name and address, then tap **Verify**.
   - When verified, the button changes to **Verified ✅**.
4. **Choose How to Start**:
   - **Start Active Journey**: Begins live GPS background mileage tracking and HUD notifications.
   - **Save Manual Entry**: Saves a completed historical load directly to your records without turning on live GPS tracking.

---

## 📷 How to Auto-Fill Loads Using OCR Screenshots (Pro)

Skip manual typing by scanning your dispatch confirmations directly:

1. **Take a Screenshot**:
   - Snap a screenshot of your rate confirmation, dispatch email, or load board app on your phone.
2. **Tap `📷 Auto-Fill from Screenshot (Pro)`**:
   - On the **New Freight Load** screen, tap the scanner button.
3. **Select Your Screenshot**:
   - Choose the screenshot from your photo gallery.
4. **Automatic Form Population**:
   - Google ML Kit on-device machine learning scans the image offline in under half a second and automatically populates:
     - **PRO Number**
     - **Gross Load Pay**
     - **Dispatched Bounce & Loaded Miles**
     - **Shipper Name & Address**
     - **Consignee Name & Address**
5. **Review & Save**:
   - Verify the pre-filled fields and tap **Start Journey** or **Save Manual Entry**.

---

## 🛰️ Real-Time GPS Mileage Tracking & Notification HUD

Load Tracker Pro uses high-accuracy GPS to measure your actual driving distance and separate your mileage segments:

* **Bounce vs. Loaded Miles**:
  - **ACTIVE_BOUNCE** (Deadhead): Measures distance driven while empty to pick up the load.
  - **ACTIVE_LOADED**: Measures distance driven with freight on your flatbed trailer.
  - Switch segments anytime by tapping **Switch to Loaded Segment** or **Arrived at Shipper/Consignee**.
* **Notification Shade HUD**:
  - Pull down your phone's notification shade while driving to monitor your live odometer, active trip segment, and distance to destination in real time—no need to keep the app open on your screen.
* **Hands-Free Arrival Geofencing**:
  - The app sets a 1,000-foot virtual fence around your shipper and consignee. Upon crossing the property line, your phone vibrates and automatically alerts you of your arrival.
* **Auto-Resume**:
  - If your phone restarts or the app is closed mid-trip, GPS tracking automatically resumes seamlessly.

---

## 💰 Flatbed Payroll Engine & Statements

Load Tracker Pro automatically calculates your estimated driver earnings based on flatbed industry rules:

* **Friday-to-Thursday Payroll Cycle**:
  - Loads picked up from **Friday through Thursday** are automatically grouped into the statement week ending on that Friday.
  - Example: A load picked up on Friday 08/28 belongs to the pay week ending Friday 09/04.
* **Itemized Statement Summary**:
  - Tap **View Statements by Week** on the main dashboard to review complete payroll statements broken down by:
    - **Total Truck Gross Revenue**: Sum of gross freight pay.
    - **Driver Load Cut**: Your percentage split (e.g. 31%).
    - **Tarp Pay**: Configurable rates for 8' Drop ($50.00) and 4' Drop ($30.00) tarps, automatically halved if marked pre-tarped.
    - **Deadhead Pay**: Automatic $0.20/mile bonus for dispatched deadhead of 150 miles or more.
    - **Trainer Pay**: Flat $200.00 weekly premium when training is active.
    - **Total Net Pay (Est)**: Calculated total driver take-home pay.
* **Main Dashboard View**:
  - Displays completed loads for the **current active payroll week** (ordered newest first).
  - All past weeks are archived under **View Statements by Week**.

---

## 🛣️ Out-Of-Route (OOR) & Going Home Protection

Protect your driver performance metrics with intelligent route auditing:

* **OOR Audit Calculation**:
  - Compares your **Actual GPS Miles** driven against your **Dispatched Miles** to calculate routing efficiency percentage.
* **Going Home Protection**:
  - When driving home on a load, check **Going Home / Home Run**.
  - Extra miles driven to your house are excluded from your Out-Of-Route penalty calculation, keeping your performance stats protected.
* **Friday Reminder**:
  - If you start a load on a Friday, the app automatically prompts you to check if it's a "Going Home" run to ensure your OOR stats stay safe.

---

## 🗺️ Route Maps, Heatmaps & Home Base Pinning

Visual lane coverage and route playback:

* **Individual Route Playback**: Tap **View Map** on any load to see the exact path you drove, including pickup, delivery, and home base markers.
* **Global Lane Heatmap**: Tap **View Global Route Heatmap** in the sidebar to see every mile you've ever tracked on a single national map.
* **Gesture Lock**: Swiping and dragging on map screens interacts 100% with map navigation without accidental sidebar menu pulls.
* **Home Base Pinning**:
  - Open the sidebar to paste your home address (e.g. `123 Main St, Dallas, TX 75201`) or tap **Pin Current** to save your GPS coordinates.
  - Your home base is displayed as a personal safe zone marker on all maps.

---

## 🏢 Facility Insights (Pro)

Your personal database of every shipper and receiver you've ever visited:

* **Search Shippers & Receivers**: Open **Review Facility Insights** in the sidebar to look up any facility by name (e.g. "Gerdau Steel").
* **Review Past Notes**: Instantly view past notes for gate codes, tight turns, loading procedures, or parking tips.
* **Easy Back Navigation**: Tap **← All Facilities** or the back button at any time to return to the facility list.

---

## 🛠️ Developer Options & Bug Tracker

* **Developer Mode**: Toggle Pro feature unlocks in **🛠️ Developer Options** at the bottom of the sidebar.
* **Developer Notes Page**: Access a full-screen scratchpad to jot down bugs and ideas saved locally to `dev_notes.txt`.
* **GitHub Issues Sync**: Push bug reports and feature requests directly to GitHub as official repository issues.

---

## ❓ Frequently Asked Questions & Troubleshooting

* **How do I fix a mistake on a trip?**:
  - If the trip is active, tap **Update / Options** to edit details. Once a trip is finished, PRO # is locked to protect database integrity.
* **How do I delete a mistake or test load?**:
  - **Long-Press** on any load card in your logs or statement view. A confirmation popup will appear to permanently delete the entry.
* **How do I export my history for tax or accounting?**:
  - Open the sidebar menu and tap **Export Payload History (CSV)** to generate a spreadsheet file you can email or save.
* **Location Tracking Issues?**:
  - Ensure Location permission is set to **"Allow all the time"** in your phone's Android Settings and battery optimization is turned off for Load Tracker Pro.
