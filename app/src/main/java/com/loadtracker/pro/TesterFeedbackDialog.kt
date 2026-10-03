package com.loadtracker.pro

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun TesterFeedbackDialog(
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { ctx.getSharedPreferences("dev_prefs", Context.MODE_PRIVATE) }

    val savedToken = prefs.getString("gh_token", "") ?: ""
    val token = if (savedToken.isNotBlank()) savedToken else BuildConfig.DEFAULT_GITHUB_TOKEN
    val repo = prefs.getString("gh_repo", BuildConfig.DEFAULT_GITHUB_REPO) ?: BuildConfig.DEFAULT_GITHUB_REPO

    var selectedCategory by remember { mutableStateOf("bug") } // "bug", "enhancement", "feedback"
    var summaryText by remember { mutableStateOf("") }
    var detailsText by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }

    var isSubmitting by remember { mutableStateOf(false) }
    var showThankYouDialog by remember { mutableStateOf(false) }

    if (showThankYouDialog) {
        AlertDialog(
            onDismissRequest = {
                showThankYouDialog = false
                onDismiss()
            },
            icon = {
                Icon(
                    Icons.Default.ThumbUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = { Text("Thank You! 🙏") },
            text = {
                Text(
                    "Your report has been submitted directly to our engineering team. Thank you for helping us improve Load Tracker Pro!",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showThankYouDialog = false
                        onDismiss()
                    }
                ) {
                    Text("OK")
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("🐛 Report Bug / Feedback") },
            text = {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Encountered a problem or have an idea? Let us know so we can make Load Tracker Pro better!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Text("Feedback Type:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategory == "bug",
                            onClick = { selectedCategory = "bug" },
                            label = { Text("Bug 🐛") }
                        )
                        FilterChip(
                            selected = selectedCategory == "enhancement",
                            onClick = { selectedCategory = "enhancement" },
                            label = { Text("Idea 💡") }
                        )
                        FilterChip(
                            selected = selectedCategory == "feedback",
                            onClick = { selectedCategory = "feedback" },
                            label = { Text("General ❓") }
                        )
                    }

                    OutlinedTextField(
                        value = summaryText,
                        onValueChange = { summaryText = it },
                        label = { Text("Summary / What happened? *") },
                        placeholder = { Text("e.g. Map didn't center after shipper arrival") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = detailsText,
                        onValueChange = { detailsText = it },
                        label = { Text("Details & Steps to Reproduce") },
                        placeholder = { Text("Provide details or comments...") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = contactEmail,
                        onValueChange = { contactEmail = it },
                        label = { Text("Your Email (Optional)") },
                        placeholder = { Text("driver@example.com (for follow-up)") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (summaryText.isBlank()) {
                            Toast.makeText(ctx, "Please enter a summary of the issue.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isSubmitting = true
                        scope.launch {
                            val categoryName = when (selectedCategory) {
                                "bug" -> "Bug Report 🐛"
                                "enhancement" -> "Feature Suggestion 💡"
                                else -> "General Feedback ❓"
                            }
                            val emailInfo = if (contactEmail.isNotBlank()) contactEmail.trim() else "Not Provided"

                            val formattedBody = """
                                ### 📋 Tester Feedback Report
                                **Category**: $categoryName
                                **Contact Email**: $emailInfo
                                
                                ### Details
                                ${detailsText.ifBlank { summaryText }}
                                
                                ---
                                *Submitted via Load Tracker Pro In-App Feedback Tool (v${BuildConfig.VERSION_NAME})*
                            """.trimIndent()

                            val appVer = if (BuildConfig.VERSION_NAME.startsWith("2")) "v2.0" else "v1.x"
                            val (success, message) = submitTesterFeedback(
                                repo = repo,
                                token = token,
                                title = "[$appVer] [$categoryName] ${summaryText.trim()}",
                                body = formattedBody,
                                labels = listOf(selectedCategory, appVer)
                            )
                            isSubmitting = false
                            if (success) {
                                showThankYouDialog = true
                            } else {
                                Toast.makeText(ctx, message, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !isSubmitting && summaryText.isNotBlank()
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
                        Text("Submit Feedback")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}

private suspend fun submitTesterFeedback(
    repo: String,
    token: String,
    title: String,
    body: String,
    labels: List<String>
): Pair<Boolean, String> = withContext(Dispatchers.IO) {
    try {
        val cleanRepo = repo.trim().removePrefix("https://github.com/").removeSuffix(".git")
        if (cleanRepo.isBlank() || token.trim().isBlank()) {
            return@withContext Pair(false, "Feedback service offline: No GitHub Token configured. Set one in Dev Notes settings.")
        }
        val url = URL("https://api.github.com/repos/$cleanRepo/issues")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Authorization", "Bearer ${token.trim()}")
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.setRequestProperty("Content-Type", "application/json; utf-8")
        conn.setRequestProperty("User-Agent", "LoadTrackerPro")
        conn.doOutput = true

        val labelsJson = labels.joinToString(separator = "\",\"", prefix = "[\"", postfix = "\"]")
        val jsonPayload = """
            {
              "title": "${escapeJson(title)}",
              "body": "${escapeJson(body)}",
              "labels": $labelsJson
            }
        """.trimIndent()

        conn.outputStream.use { os ->
            os.write(jsonPayload.toByteArray(Charsets.UTF_8))
        }

        val responseCode = conn.responseCode
        if (responseCode in 200..299) {
            Pair(true, "Feedback submitted successfully!")
        } else {
            val errText = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
            Pair(false, "Could not submit feedback ($responseCode): $errText")
        }
    } catch (e: Exception) {
        Pair(false, "Network error: ${e.localizedMessage}")
    }
}

private fun escapeJson(str: String): String {
    return str.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}
