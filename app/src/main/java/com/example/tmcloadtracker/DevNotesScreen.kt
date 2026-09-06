package com.example.tmcloadtracker

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class GitHubIssue(
    val number: Int,
    val title: String,
    val state: String,
    val body: String,
    val author: String,
    val labels: List<String>,
    val createdAt: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevNotesScreen(
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { ctx.getSharedPreferences("dev_prefs", Context.MODE_PRIVATE) }

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Tab 1: Local Scratchpad
    var notesText by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }
    val devFile = remember { File(ctx.filesDir, "dev_notes.txt") }

    // Tab 2: GitHub Issues Sync
    val savedToken = prefs.getString("gh_token", "") ?: ""
    var githubRepo by remember { mutableStateOf(prefs.getString("gh_repo", "pa2122/android_apps") ?: "pa2122/android_apps") }
    var githubToken by remember {
        mutableStateOf(
            savedToken.ifBlank { BuildConfig.DEFAULT_GITHUB_TOKEN }
        )
    }
    var issueTitle by remember { mutableStateOf("") }
    var issueBody by remember { mutableStateOf("") }
    var selectedLabel by remember { mutableStateOf("bug") }
    var isPostingToGithub by remember { mutableStateOf(false) }

    var connectionStatusText by remember { mutableStateOf("⚪ Not Tested") }
    var isTestingConnection by remember { mutableStateOf(false) }

    // GitHub Issue Reader State
    var githubIssues by remember { mutableStateOf<List<GitHubIssue>>(emptyList()) }
    var isFetchingIssues by remember { mutableStateOf(false) }
    var issuesErrorText by remember { mutableStateOf("") }
    var issuesFilterState by remember { mutableStateOf("open") } // "all", "open", "closed"
    var expandedIssueNumber by remember { mutableIntStateOf(-1) }

    fun loadIssues() {
        if (githubRepo.isNotBlank()) {
            isFetchingIssues = true
            issuesErrorText = ""
            scope.launch {
                val (issues, error) = fetchGitHubIssues(githubRepo, githubToken)
                isFetchingIssues = false
                if (error.isBlank()) {
                    githubIssues = issues
                } else {
                    issuesErrorText = error
                }
            }
        }
    }

    // Load saved notes from dev_notes.txt on open
    LaunchedEffect(Unit) {
        try {
            if (devFile.exists()) {
                notesText = devFile.readText()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Auto-test GitHub connection & fetch issues on tab open
    LaunchedEffect(githubToken, githubRepo) {
        if (githubToken.isNotBlank() && githubRepo.isNotBlank()) {
            if (connectionStatusText == "⚪ Not Tested") {
                isTestingConnection = true
                connectionStatusText = "🟡 Testing Connection..."
                val (_, msg) = testGitHubConnection(githubRepo, githubToken)
                isTestingConnection = false
                connectionStatusText = msg
            }
            loadIssues()
        }
    }

    fun saveLocalNotes() {
        try {
            devFile.writeText(notesText)
            Toast.makeText(ctx, "Saved to dev_notes.txt!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(ctx, "Error saving notes: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareLocalNotes() {
        try {
            devFile.writeText(notesText)
            val contentUri = FileProvider.getUriForFile(
                ctx,
                "${ctx.packageName}.provider",
                devFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Load Tracker Pro - Developer Notes")
                putExtra(Intent.EXTRA_TEXT, notesText)
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            ctx.startActivity(Intent.createChooser(shareIntent, "Share Developer Notes"))
        } catch (e: Exception) {
            Toast.makeText(ctx, "Error sharing notes: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun saveGithubPrefs() {
        prefs.edit()
            .putString("gh_repo", githubRepo.trim())
            .putString("gh_token", githubToken.trim())
            .apply()
    }

    BackHandler {
        saveLocalNotes()
        saveGithubPrefs()
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🛠️ Developer Notes") },
                navigationIcon = {
                    IconButton(onClick = {
                        saveLocalNotes()
                        saveGithubPrefs()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectedTabIndex == 0) {
                        IconButton(onClick = { saveLocalNotes() }) {
                            Icon(Icons.Default.Done, contentDescription = "Save Notes")
                        }
                    } else {
                        IconButton(onClick = { loadIssues() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Issues")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("📝 Local Scratchpad") },
                    icon = { Icon(Icons.Default.Edit, contentDescription = null) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("🚀 GitHub Sync") },
                    icon = { Icon(Icons.Default.Build, contentDescription = null) }
                )
            }

            if (selectedTabIndex == 0) {
                // TAB 1: LOCAL SCRATCHPAD
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Jot down bugs, ideas, or notes. Automatically saved to dev_notes.txt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Developer Scratchpad (`dev_notes.txt`)") },
                        placeholder = { Text("• Bug: ...\n• Idea: ...\n• Feature Request: ...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { saveLocalNotes() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Done, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Notes")
                        }

                        Button(
                            onClick = { shareLocalNotes() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share / Export")
                        }

                        IconButton(
                            onClick = { showClearDialog = true }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Clear Notes",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            } else {
                // TAB 2: GITHUB ISSUES SYNC & READER
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Read existing repository issues and push new bugs/ideas to GitHub.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    OutlinedTextField(
                        value = githubRepo,
                        onValueChange = {
                            githubRepo = it
                            saveGithubPrefs()
                        },
                        label = { Text("GitHub Repository (owner/repo)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = githubToken,
                        onValueChange = {
                            githubToken = it
                            saveGithubPrefs()
                        },
                        label = { Text("GitHub Personal Access Token (PAT)") },
                        placeholder = { Text("ghp_... or github_pat_...") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Connection Status & Test Button Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = connectionStatusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                connectionStatusText.startsWith("🟢") -> MaterialTheme.colorScheme.primary
                                connectionStatusText.startsWith("🔴") -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.secondary
                            },
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = {
                                saveGithubPrefs()
                                isTestingConnection = true
                                connectionStatusText = "🟡 Testing Connection..."
                                scope.launch {
                                    val (_, msg) = testGitHubConnection(githubRepo, githubToken)
                                    isTestingConnection = false
                                    connectionStatusText = msg
                                    loadIssues()
                                }
                            },
                            enabled = !isTestingConnection && githubToken.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Testing...")
                            } else {
                                Text("🧪 Test Connection")
                            }
                        }
                    }

                    HorizontalDivider()

                    // READ-ONLY GITHUB ISSUE READER
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "📋 GitHub Issue Reader",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        IconButton(
                            onClick = { loadIssues() },
                            enabled = !isFetchingIssues
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Issues List")
                        }
                    }

                    // Filter Chips for Issues
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = issuesFilterState == "open",
                            onClick = { issuesFilterState = "open" },
                            label = { Text("Open (${githubIssues.count { it.state == "open" }})") }
                        )
                        FilterChip(
                            selected = issuesFilterState == "closed",
                            onClick = { issuesFilterState = "closed" },
                            label = { Text("Closed (${githubIssues.count { it.state == "closed" }})") }
                        )
                        FilterChip(
                            selected = issuesFilterState == "all",
                            onClick = { issuesFilterState = "all" },
                            label = { Text("All (${githubIssues.size})") }
                        )
                    }

                    if (isFetchingIssues) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Fetching issues from GitHub...", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else if (issuesErrorText.isNotBlank()) {
                        Text(
                            issuesErrorText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        val filteredIssues = githubIssues.filter {
                            when (issuesFilterState) {
                                "open" -> it.state == "open"
                                "closed" -> it.state == "closed"
                                else -> true
                            }
                        }

                        if (filteredIssues.isEmpty()) {
                            Text(
                                "No ${issuesFilterState} issues found for $githubRepo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            filteredIssues.forEach { issue ->
                                val isExpanded = expandedIssueNumber == issue.number
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            expandedIssueNumber = if (isExpanded) -1 else issue.number
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "#${issue.number} ${issue.title}",
                                                style = MaterialTheme.typography.titleSmall,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Icon(
                                                if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "by @${issue.author} • ${issue.createdAt}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Text(
                                                text = issue.state.uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (issue.state == "open") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                                            )
                                        }

                                        if (isExpanded) {
                                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                            Text(
                                                text = issue.body.ifBlank { "No description provided." },
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // PUSH NEW ISSUE SECTION
                    Text("➕ Push New GitHub Issue", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

                    Text("Issue Category / Label:", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedLabel == "bug",
                            onClick = { selectedLabel = "bug" },
                            label = { Text("Bug 🐛") }
                        )
                        FilterChip(
                            selected = selectedLabel == "enhancement",
                            onClick = { selectedLabel = "enhancement" },
                            label = { Text("Feature 💡") }
                        )
                        FilterChip(
                            selected = selectedLabel == "documentation",
                            onClick = { selectedLabel = "documentation" },
                            label = { Text("Doc 📝") }
                        )
                    }

                    OutlinedTextField(
                        value = issueTitle,
                        onValueChange = { issueTitle = it },
                        label = { Text("Issue Title") },
                        placeholder = { Text("e.g. Fix map zoom on tablet") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = issueBody,
                        onValueChange = { issueBody = it },
                        label = { Text("Issue Description / Body") },
                        placeholder = { Text("Detailed notes, steps to reproduce, or feature details...") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (notesText.isNotBlank() && issueBody.isBlank()) {
                        TextButton(
                            onClick = { issueBody = notesText }
                        ) {
                            Text("📋 Copy text from Local Scratchpad into Body")
                        }
                    }

                    Button(
                        onClick = {
                            saveGithubPrefs()
                            if (githubToken.isBlank()) {
                                Toast.makeText(ctx, "Please enter your GitHub Personal Access Token.", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            if (issueTitle.isBlank()) {
                                Toast.makeText(ctx, "Please enter an Issue Title.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isPostingToGithub = true
                            scope.launch {
                                val (success, message) = createGitHubIssue(
                                    repo = githubRepo,
                                    token = githubToken,
                                    title = issueTitle,
                                    body = issueBody,
                                    labels = listOf(selectedLabel)
                                )
                                isPostingToGithub = false
                                Toast.makeText(ctx, message, Toast.LENGTH_LONG).show()
                                if (success) {
                                    issueTitle = ""
                                    issueBody = ""
                                    loadIssues()
                                }
                            }
                        },
                        enabled = !isPostingToGithub && issueTitle.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isPostingToGithub) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pushing to GitHub...")
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("🚀 Push Issue to GitHub")
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Local Scratchpad?") },
            text = { Text("Are you sure you want to clear your local notes? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        notesText = ""
                        saveLocalNotes()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Clear All") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }
}

private suspend fun fetchGitHubIssues(
    repo: String,
    token: String
): Pair<List<GitHubIssue>, String> = withContext(Dispatchers.IO) {
    try {
        val cleanRepo = repo.trim().removePrefix("https://github.com/").removeSuffix(".git")
        if (cleanRepo.isBlank()) return@withContext Pair(emptyList(), "Repo name cannot be blank.")

        val url = URL("https://api.github.com/repos/$cleanRepo/issues?state=all&per_page=30")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        if (token.trim().isNotBlank()) {
            conn.setRequestProperty("Authorization", "Bearer ${token.trim()}")
        }
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.setRequestProperty("User-Agent", "LoadTrackerPro")

        val responseCode = conn.responseCode
        if (responseCode in 200..299) {
            val responseText = conn.inputStream.bufferedReader().use { it.readText() }
            val issues = parseGitHubIssuesJson(responseText)
            Pair(issues, "")
        } else {
            val errText = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
            Pair(emptyList(), "HTTP $responseCode: ${parseErrorMessage(errText)}")
        }
    } catch (e: Exception) {
        Pair(emptyList(), "Network Error: ${e.localizedMessage}")
    }
}

private fun parseGitHubIssuesJson(jsonText: String): List<GitHubIssue> {
    val list = mutableListOf<GitHubIssue>()
    try {
        val items = jsonText.trim().removePrefix("[").removeSuffix("]").split(Regex("""\}\s*,\s*\{(?=\s*"url")"""))
        for (item in items) {
            val numMatch = Regex(""""number"\s*:\s*(\d+)""").find(item)
            val titleMatch = Regex(""""title"\s*:\s*"([^"\\]*(?:\\.[^"\\]*)*)"""").find(item)
            val stateMatch = Regex(""""state"\s*:\s*"([^"]+)"""").find(item)
            val bodyMatch = Regex(""""body"\s*:\s*"([^"\\]*(?:\\.[^"\\]*)*)"""").find(item)
            val authorMatch = Regex(""""login"\s*:\s*"([^"]+)"""").find(item)
            val createdMatch = Regex(""""created_at"\s*:\s*"([^"]+)"""").find(item)

            if (numMatch != null && titleMatch != null) {
                val num = numMatch.groupValues[1].toIntOrNull() ?: 0
                val title = unescapeJson(titleMatch.groupValues[1])
                val state = stateMatch?.groupValues?.get(1) ?: "open"
                val body = if (bodyMatch != null) unescapeJson(bodyMatch.groupValues[1]) else ""
                val author = authorMatch?.groupValues?.get(1) ?: "github"
                val rawCreated = createdMatch?.groupValues?.get(1) ?: ""
                val formattedDate = if (rawCreated.length >= 10) rawCreated.substring(0, 10) else rawCreated

                val labelMatches = Regex(""""name"\s*:\s*"([^"]+)"""").findAll(item)
                val labels = labelMatches.map { it.groupValues[1] }.toList()

                list.add(
                    GitHubIssue(
                        number = num,
                        title = title,
                        state = state,
                        body = body,
                        author = author,
                        labels = labels,
                        createdAt = formattedDate
                    )
                )
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}

private fun unescapeJson(str: String): String {
    return str.replace("\\\"", "\"")
        .replace("\\n", "\n")
        .replace("\\r", "\r")
        .replace("\\t", "\t")
        .replace("\\\\", "\\")
}

private suspend fun testGitHubConnection(
    repo: String,
    token: String
): Pair<Boolean, String> = withContext(Dispatchers.IO) {
    try {
        val cleanRepo = repo.trim().removePrefix("https://github.com/").removeSuffix(".git")
        if (cleanRepo.isBlank() || token.trim().isBlank()) {
            return@withContext Pair(false, "Repo and Token cannot be blank.")
        }
        val url = URL("https://api.github.com/repos/$cleanRepo")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Authorization", "Bearer ${token.trim()}")
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.setRequestProperty("User-Agent", "LoadTrackerPro")

        val responseCode = conn.responseCode
        if (responseCode in 200..299) {
            val responseText = conn.inputStream.bufferedReader().use { it.readText() }
            val repoFullName = Regex(""""full_name"\s*:\s*"([^"]+)"""").find(responseText)?.groupValues?.get(1) ?: cleanRepo
            Pair(true, "🟢 Connected: $repoFullName")
        } else {
            val errText = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
            Pair(false, "🔴 Failed ($responseCode): ${parseErrorMessage(errText)}")
        }
    } catch (e: Exception) {
        Pair(false, "🔴 Error: ${e.localizedMessage}")
    }
}

private suspend fun createGitHubIssue(
    repo: String,
    token: String,
    title: String,
    body: String,
    labels: List<String>
): Pair<Boolean, String> = withContext(Dispatchers.IO) {
    try {
        val cleanRepo = repo.trim().removePrefix("https://github.com/").removeSuffix(".git")
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
            val responseText = conn.inputStream.bufferedReader().use { it.readText() }
            val issueNum = Regex(""""number"\s*:\s*(\d+)""").find(responseText)?.groupValues?.get(1) ?: ""
            Pair(true, "GitHub Issue #$issueNum created successfully!")
        } else {
            val errText = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
            Pair(false, "GitHub API Error ($responseCode): ${parseErrorMessage(errText)}")
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

private fun parseErrorMessage(jsonText: String): String {
    val msgMatch = Regex(""""message"\s*:\s*"([^"]+)"""").find(jsonText)
    return msgMatch?.groupValues?.get(1) ?: "Check your token and repository name."
}
