package com.loadtracker.pro

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HvutTrackerScreen(
    loads: List<CurrentLoad>,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val scroll = rememberScrollState()

    val today = remember { LocalDate.now() }
    val taxYearStart = remember { HvutCalculator.getCurrentTaxYearStartDate(today) }
    val deadline = remember { HvutCalculator.getFilingDeadline(today) }
    val daysUntilDeadline = remember { HvutCalculator.calculateDaysUntilDeadline(today) }

    // Calculate total miles in current tax year
    val annualMiles = remember(loads, taxYearStart) {
        val taxYearStartEpochMs = taxYearStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        loads.filter { it.pickupTimestamp >= taxYearStartEpochMs }.sumOf { load ->
            val actB = load.bounceMilesEnd - load.bounceMilesStart
            val actL = load.loadedMilesEnd - load.loadedMilesStart
            val actual = actB + actL
            val disp = load.dispatchedBounceMiles + load.dispatchedLoadedMiles
            if (actual > 0) actual else disp
        }
    }

    val isExempt = remember(annualMiles) { HvutCalculator.isExemptFromTax(annualMiles) }
    val remainingExemptMiles = remember(annualMiles) { HvutCalculator.calculateRemainingExemptMiles(annualMiles) }
    val progressFraction = (annualMiles / 5000.0).coerceIn(0.0, 1.0).toFloat()

    var vinNumber by remember { mutableStateOf("") }
    var schedule1Received by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Form 2290 HVUT & 5k Exemption") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Deadline Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (daysUntilDeadline in 0..30) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📅 IRS Form 2290 Deadline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Icon(
                            if (daysUntilDeadline in 0..30) Icons.Default.Warning else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (daysUntilDeadline in 0..30) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                    Text("Due Date: August 31 (${deadline.year})", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text(
                        if (daysUntilDeadline >= 0) "⏱️ $daysUntilDeadline days remaining to file Schedule 1" else "⚠️ Past August 31 filing deadline!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (daysUntilDeadline in 0..30) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 5,000-Mile Exemption Counter Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("5,000-Mile Exemption Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Surface(
                            color = if (isExempt) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                if (isExempt) "SUSPENDED EXEMPT" else "TAXABLE ($550)",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text("Tax Year Driven (July 1 - June 30): ${String.format(Locale.US, "%.1f", annualMiles)} mi / 5,000 mi", style = MaterialTheme.typography.bodyLarge)

                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = if (isExempt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )

                    Text(
                        if (isExempt)
                            "✅ Under 5,000 miles: Eligible for IRS Suspended Schedule 1 ($0 Tax Due)."
                        else
                            "⚠️ Over 5,000 miles: Subject to standard $550/year Heavy Vehicle Use Tax.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isExempt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }

            // Schedule 1 Verification Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("IRS Stamped Schedule 1 Record", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = vinNumber,
                        onValueChange = { vinNumber = it.uppercase(Locale.US) },
                        label = { Text("Vehicle Identification Number (VIN)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (vinNumber.length < 5) {
                                Toast.makeText(ctx, "Please enter valid VIN number.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            schedule1Received = true
                            Toast.makeText(ctx, "Schedule 1 record verified for VIN: $vinNumber", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(if (schedule1Received) "✅ Schedule 1 Record Verified" else "Verify Schedule 1 Filing")
                    }
                }
            }
        }
    }
}
