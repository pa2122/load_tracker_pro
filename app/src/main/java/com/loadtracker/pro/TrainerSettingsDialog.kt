package com.loadtracker.pro

import java.util.Locale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun TrainerSettingsDialog(
    initialIsActive: Boolean,
    initialTier: String, // "inexperienced", "experienced", "custom"
    initialWeek: Int, // 1, 2, 3, 4
    initialIsBoostActive: Boolean,
    initialCustomRate: String,
    onSave: (isActive: Boolean, tier: String, week: Int, isBoostActive: Boolean, customRate: String, calculatedPay: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var isActive by remember { mutableStateOf(initialIsActive) }
    var tier by remember { mutableStateOf(initialTier) }
    var week by remember { mutableIntStateOf(initialWeek) }
    var isBoostActive by remember { mutableStateOf(initialIsBoostActive) }
    var customRate by remember { mutableStateOf(initialCustomRate) }

    val basePay = calculateBaseTrainerPay(tier, week, customRate.toDoubleOrNull() ?: 200.0)
    val boostPay = if (isBoostActive && tier != "custom") 100.0 else 0.0
    val totalCalculatedPay = if (isActive) basePay + boostPay else 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🎓 Trainer Incentive Settings") },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Trainer Mode", style = MaterialTheme.typography.titleMedium)
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it }
                    )
                }

                if (isActive) {
                    Text("Trainee Program Tier:", style = MaterialTheme.typography.labelMedium)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = tier == "inexperienced",
                            onClick = { tier = "inexperienced" },
                            label = { Text("Inexperienced (4-Week Program)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        FilterChip(
                            selected = tier == "experienced",
                            onClick = {
                                tier = "experienced"
                                if (week > 2) week = 2
                            },
                            label = { Text("Experienced (2-Week Program)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        FilterChip(
                            selected = tier == "custom",
                            onClick = { tier = "custom" },
                            label = { Text("Custom Weekly Rate") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (tier != "custom") {
                        Text("Current Training Week:", style = MaterialTheme.typography.labelMedium)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = week == 1,
                                    onClick = { week = 1 },
                                    label = { Text("Week 1") },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = week == 2,
                                    onClick = { week = 2 },
                                    label = { Text("Week 2") },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (tier == "inexperienced") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = week == 3,
                                        onClick = { week = 3 },
                                        label = { Text("Week 3") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = week == 4,
                                        onClick = { week = 4 },
                                        label = { Text("Week 4") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("TMC Promotional Boost", style = MaterialTheme.typography.bodyMedium)
                                Text("+$100.00/wk (Until further notice)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = isBoostActive,
                                onCheckedChange = { isBoostActive = it }
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = customRate,
                            onValueChange = { customRate = it },
                            label = { Text("Custom Weekly Trainer Pay ($)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Weekly Trainer Pay Breakdown", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Base Rate:", style = MaterialTheme.typography.bodySmall)
                                Text("$${String.format(Locale.US, "%.2f", basePay)}", style = MaterialTheme.typography.bodySmall)
                            }
                            if (boostPay > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("TMC Promo Boost:", style = MaterialTheme.typography.bodySmall)
                                    Text("+$${String.format(Locale.US, "%.2f", boostPay)}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Trainer Pay Added:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("+$${String.format(Locale.US, "%.2f", totalCalculatedPay)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(isActive, tier, week, isBoostActive, customRate, totalCalculatedPay)
                }
            ) {
                Text("Save Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun calculateBaseTrainerPay(tier: String, week: Int, customRate: Double): Double {
    return when (tier) {
        "inexperienced" -> if (week in 1..2) 200.0 else 100.0
        "experienced" -> 100.0
        else -> customRate
    }
}
