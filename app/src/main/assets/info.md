# TMC Load Tracker - Upcoming Features Scratchpad

## 🗑️ Feature: Delete Record Query Logic

### 1. Add to LoadDao.kt

```kotlin
@Delete
private suspend fun deleteLoad(load: CurrentLoad)
```

### 2. Add to LoadViewModel.kt

```kotlin
fun deleteLoad(loadOriginal: CurrentLoad) {
    viewModelScope.launch {
        loadDao.deleteLoad(loadOriginal)
    }
}
```

### 3. Add to MainActivity.kt (Inside "dashboard" block)

```kotlin
onDeleteTripClick = { loadToDelete -> viewModel.deleteLoad(loadOriginal = loadToDelete) }
```

### 4. Add to DashboardScreen.kt (History Card Loop)

```kotlin
IconButton(onClick = { onDeleteTripClick(load) }) {
    Icon(
        imageVector = Icons.Default.Delete,
        contentDescription = "Delete",
        tint = MaterialTheme.colorScheme.error
    )
}
```
# TMC Load Tracker - Owner-Operator Business Suite Blueprint

## ⛽ 1. Dynamic Fuel Cost & IFTA Tracker
*   **Fuel Stop Expense Input**: Add entry fields to log fuel gallons, total cost, and the state purchase location whenever you top off your tanks.
*   **Cost-per-Mile Impact**: Automatically subtract your total fuel spending from your weekly pay summary to show your real-time net take-home cash.
*   **IFTA State Log**: Since the app tracks GPS distance by state, compile a simple report showing total miles driven per state vs. gallons purchased for quarterly IFTA filing paperwork.

## 🛠️ 2. Fixed & Variable Operating Cost Sheet
*   **Fixed Costs (The Overhead)**: Add configuration boxes for your structural weekly fixed costs (e.g., truck payment, physical damage insurance, ELD subscription, permit fees).
*   **Variable Costs (The Maintenance)**: Add a rolling deduction field for variable costs (e.g., setting aside $0.10 or $0.15 per mile into an escrow/maintenance account for tires and oil changes).
*   **The Bottom Line Calculator**: Deduct these values from your weekly gross alongside your fuel logs to track your exact **Net Operating Profit**.

## 📊 3. Key Performance Indicators (KPI) Dashboard
*   **All-Inclusive Revenue Per Mile (RPM)**: Display your total weekly revenue divided by your total driven miles (e.g., "Grossing $3.10/mile").
*   **Cost Per Mile (CPM)**: Display your total combined expenses (fixed + variable + fuel) divided by your total driven miles (e.g., "Costing $1.85/mile to run the truck").
*   **Net Profit Per Mile**: Show the metric that matters most: your RPM minus your CPM (e.g., "Pocketing $1.25/mile net").

## 🗂️ 4. Carrier Deductions Panel
*   **Escrow & Fee Offsets**: Add toggles for standard lease-purchase or owner-operator carrier deductions (such as trailer lease fees, occupational accident insurance, or company dispatch fees).
*   **Accurate Settlement Projections**: Ensure your settlement screen popup precisely mimics your carrier's physical weekly check settlement sheet.
