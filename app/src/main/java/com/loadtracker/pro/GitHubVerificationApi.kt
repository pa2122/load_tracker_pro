package com.loadtracker.pro

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class GitHubVerificationResult(
    val success: Boolean,
    val message: String,
    val issue: GitHubIssue? = null
)

object GitHubVerificationApi {
    suspend fun updateStep(
        repo: String,
        token: String,
        issueNumber: Int,
        version: String,
        stepIndex: Int,
        expectedStepText: String,
        isComplete: Boolean
    ): GitHubVerificationResult = withContext(Dispatchers.IO) {
        val current = requestIssue(repo, token, issueNumber)
        if (!current.success || current.issue == null) return@withContext current
        if (current.issue.state != "open") {
            return@withContext GitHubVerificationResult(false, "Issue #$issueNumber is no longer open.")
        }

        val updatedBody = VerificationChecklist.updateStep(
            body = current.issue.body,
            version = version,
            stepIndex = stepIndex,
            isComplete = isComplete,
            expectedStepText = expectedStepText
        ) ?: return@withContext GitHubVerificationResult(
            false,
            "Checklist changed on GitHub. Refresh the issue list and try again."
        )

        patchIssue(repo, token, issueNumber, JSONObject().put("body", updatedBody))
    }

    suspend fun closeAsVerified(
        repo: String,
        token: String,
        issueNumber: Int
    ): GitHubVerificationResult = withContext(Dispatchers.IO) {
        val current = requestIssue(repo, token, issueNumber)
        if (!current.success || current.issue == null) return@withContext current
        val issue = current.issue
        if (issue.state != "open") {
            return@withContext GitHubVerificationResult(false, "Issue #$issueNumber is no longer open.")
        }
        if (!issue.labels.contains("needs-verification")) {
            return@withContext GitHubVerificationResult(
                false,
                "Issue #$issueNumber no longer has the needs-verification label."
            )
        }
        if (!VerificationChecklist.allApplicableVersionsComplete(issue.body)) {
            return@withContext GitHubVerificationResult(
                false,
                "All applicable version checklists must be READY and complete before closing."
            )
        }

        val comment = buildString {
            appendLine("Verified using the in-app checklist.")
            VerificationChecklist.sections(issue.body)
                .filter { it.status != "N/A" }
                .forEach { section ->
                    appendLine()
                    appendLine("**${section.version} device verification:**")
                    section.steps.forEach { appendLine("- ${it.text}") }
                }
            append("<!-- load-tracker-verification-${issueNumber} -->")
        }

        val commentResult = postVerificationComment(repo, token, issueNumber, comment)
        if (!commentResult.success) return@withContext commentResult

        val labels = issue.labels.filterNot { it == "needs-verification" }
            .toMutableSet()
            .apply { add("verified") }
        val payload = JSONObject()
            .put("state", "closed")
            .put("state_reason", "completed")
            .put("labels", JSONArray(labels.toList()))

        patchIssue(repo, token, issueNumber, payload).let { result ->
            if (result.success) {
                result.copy(message = "Issue #$issueNumber marked verified and closed.")
            } else {
                result.copy(message = "Verification comment posted, but closing failed: ${result.message}")
            }
        }
    }

    private fun requestIssue(repo: String, token: String, number: Int): GitHubVerificationResult {
        val response = sendRequest(repo, token, "GET", "issues/$number")
        if (!response.success) return response
        return try {
            val issue = JSONObject(response.message)
            GitHubVerificationResult(
                success = true,
                message = "",
                issue = GitHubIssue(
                    number = issue.getInt("number"),
                    title = issue.optString("title"),
                    state = issue.optString("state"),
                    body = issue.optString("body"),
                    author = issue.optJSONObject("user")?.optString("login").orEmpty(),
                    labels = issue.optJSONArray("labels").toLabelNames(),
                    createdAt = issue.optString("created_at").take(10)
                )
            )
        } catch (e: Exception) {
            GitHubVerificationResult(false, "Unable to read GitHub issue: ${e.localizedMessage}")
        }
    }

    private fun patchIssue(
        repo: String,
        token: String,
        number: Int,
        payload: JSONObject
    ): GitHubVerificationResult {
        val response = sendRequest(repo, token, "PATCH", "issues/$number", payload)
        if (!response.success) return response
        return try {
            val issue = JSONObject(response.message)
            GitHubVerificationResult(
                success = true,
                message = "Checklist saved to GitHub.",
                issue = GitHubIssue(
                    number = issue.getInt("number"),
                    title = issue.optString("title"),
                    state = issue.optString("state"),
                    body = issue.optString("body"),
                    author = issue.optJSONObject("user")?.optString("login").orEmpty(),
                    labels = issue.optJSONArray("labels").toLabelNames(),
                    createdAt = issue.optString("created_at").take(10)
                )
            )
        } catch (e: Exception) {
            GitHubVerificationResult(false, "GitHub updated the issue but returned an unreadable response.")
        }
    }

    private fun postVerificationComment(
        repo: String,
        token: String,
        number: Int,
        comment: String
    ): GitHubVerificationResult {
        val existingComments = sendRequest(repo, token, "GET", "issues/$number/comments?per_page=100")
        if (!existingComments.success) {
            return existingComments.copy(message = "Could not check existing verification comments: ${existingComments.message}")
        }
        val alreadyPosted = try {
            val comments = JSONArray(existingComments.message)
            (0 until comments.length()).any { index ->
                comments.getJSONObject(index).optString("body")
                    .contains("<!-- load-tracker-verification-$number -->")
            }
        } catch (_: Exception) {
            false
        }
        if (alreadyPosted) return GitHubVerificationResult(true, "Verification comment already exists.")

        return sendRequest(
            repo,
            token,
            "POST",
            "issues/$number/comments",
            JSONObject().put("body", comment)
        )
    }

    private fun sendRequest(
        repo: String,
        token: String,
        method: String,
        path: String,
        payload: JSONObject? = null
    ): GitHubVerificationResult {
        val cleanRepo = repo.trim()
            .removePrefix("https://github.com/")
            .removeSuffix(".git")
            .trimEnd('/')
        if (!Regex("""^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$""").matches(cleanRepo)) {
            return GitHubVerificationResult(false, "Repository must be in owner/repo format.")
        }
        if (token.isBlank()) {
            return GitHubVerificationResult(false, "Enter a GitHub token with permission to update issues.")
        }

        return try {
            val connection = URL("https://api.github.com/repos/$cleanRepo/$path")
                .openConnection() as HttpURLConnection
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("Authorization", "Bearer ${token.trim()}")
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            connection.setRequestProperty("User-Agent", "LoadTrackerPro")

            if (payload != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val responseBody = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            connection.disconnect()
            if (code in 200..299) {
                GitHubVerificationResult(true, responseBody)
            } else {
                val message = try {
                    JSONObject(responseBody).optString("message").ifBlank { "HTTP $code" }
                } catch (_: Exception) {
                    "HTTP $code"
                }
                GitHubVerificationResult(false, "GitHub API error ($code): $message")
            }
        } catch (e: Exception) {
            GitHubVerificationResult(false, "GitHub request failed: ${e.localizedMessage}")
        }
    }

    private fun JSONArray?.toLabelNames(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { index ->
            optJSONObject(index)?.optString("name")?.takeIf { it.isNotBlank() }
        }
    }
}
