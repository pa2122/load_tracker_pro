# 🎨 Load Tracker Pro v2.0 — Design Vision & Architecture Roadmap

*This document outlines the UI/UX design system, navigation flow, custom vector assets, and feature specifications for the Version 2.0 release of Load Tracker Pro (`load_tracker_pro`).*

---

## 🎨 1. Brand Identity & Design System

### **Color Palette (High-Contrast Night/Day Driver Theme)**
- 🌑 **Primary (Midnight Steel / Deep Navy):** `#0A192F`  
  *Dominant dark background—easy on the eyes during night runs and reduces cab glare.*
- ⚡ **Accent Highlight (Safety Amber / Chrome Yellow):** `#FFB300`  
  *Bold, high-visibility contrast for PRO numbers, active trip status, and net pay figures.*
- 🟢 **Success Accent (Emerald Green):** `#00C853`  
  *Used for completed trips, positive pay cuts, and verified facility geofences.*
- 📦 **Surface Variant (Charcoal Cards):** `#1E293B`  
  *Sleek, rounded cards with crisp borders.*

---

## 🎨 2. Custom Iconography (`res/drawable/ic_flatbed_*.xml`)

To give Load Tracker Pro a custom, professional flatbed trucking feel, generic icons will be replaced with custom resolution-independent **Android Vector Drawables**:

- 🚛 `ic_semi_truck.xml` *(Sleek aerodynamic semi tractor)*
- 🛞 `ic_flatbed_trailer.xml` *(Flatbed trailer with strap/chain icons)*
- 🪢 `ic_tarp_drop.xml` *(4' / 8' tarping icon)*
- ⛽ `ic_diesel_pump.xml` *(Fuel nozzle icon for IFTA logger)*
- ⚖️ `ic_weigh_station.xml` *(Scale house / WIM icon)*
- 📄 `ic_pay_stub.xml` *(Settlement statement & paycheck icon)*

### **How Custom Vector Assets Are Added:**
1. Design or download vector icons in SVG format (e.g. from Material Symbols, Figma, or Illustrator).
2. Right-click `app/src/main/res/drawable` ➔ **New ➔ Vector Asset**.
3. Select **Local file (SVG)** to automatically generate resolution-independent XML vector assets.

---

## 🧭 3. App Navigation Flow & Workspace Structure

v2.0 organizes Load Tracker Pro into **4 Core Workspaces** accessible via a sleek **Material 3 Bottom Navigation Bar**:

```
 ┌─────────────────────────────────────────────────────────────┐
 │  [🏠 Dashboard]   [🗺️ Live Map]   [⛽ Fuel]   [📊 Ledger]   │
 └─────────────────────────────────────────────────────────────┘
```

### **Workspace 1: 🏠 Live Dashboard (`DashboardScreen`)**
- **Hero Card:** Massive active load banner showing current segment (`Bounce` vs `Loaded`), live OOR %, PRO #, and 1-tap action buttons (`Arrive at Shipper`, `Depart Loaded`, `Launch Navigation`).
- **Weekly Pay Summary Bar:** Real-time paycheck ticker showing your week-ending gross cut, tarp pay, deadhead bonus, and trainer pay.

### **Workspace 2: 🗺️ Live Map & Facility Insights (`RouteMapScreen`)**
- Fullscreen Google Map rendering 2.5-minute GPS breadcrumbs, historical route lines, and shipper/consignee pins.
- Tapping a pin reveals facility notes, overnight parking rules, and the 1-tap `📞 Call Shipping Office` button.

### **Workspace 3: ⛽ Fuel Logger & IFTA Tracker (`FuelLoggerScreen`)**
- Quick 3-second fuel entry (Gallons, Total Cost, State, Station, Odometer).
- Real-time MPG analytics, cost per gallon, and state-by-state fuel tax summaries (Roadmap Issue #44 / #12).

### **Workspace 4: 📊 Payroll & Owner-Op Profit Ledger (`LedgerScreen`)**
- Filter historical loads by week-ending Friday.
- Revenue Per Mile (RPM), Cost Per Mile (CPM), and Net Take-Home Statement generation (Roadmap Issue #3).

---

## 🎯 4. Milestone Issue Mapping

All v2.0 tasks are assigned to GitHub Milestone **`v2.0 Owner-Op & Monetization Redesign`**:
- **#3:** Owner Op Business Suite upgrade
- **#11:** Implement Cloud Storage and Database Backups
- **#12:** IFTA State-by-State Mileage Tracking
- **#13:** Fuel & DEF Expense Logger + MPG Analytics
- **#14:** Professional Load Resume (PDF Export)
- **#24:** Implement Google Play Billing & Feature Gating
- **#25:** Integrate Google Play Billing Library (`billing-ktx`)
- **#44:** Quick Fuel Logger & State Mileage Tracker for IFTA Analytics
- **#63:** Form 2290 HVUT Renewal Tracker & 5,000-Mile Exemption Counter
