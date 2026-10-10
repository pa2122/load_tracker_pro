package com.loadtracker.pro

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.core.content.edit
import com.google.mlkit.vision.text.Text
import kotlin.math.abs

object OcrHelper {

    private const val PREFS_NAME = "app_settings"
    private const val KEY_AUTO_DELETE = "auto_delete_screenshots"

    /**
     * Reconstructs raw text from ML Kit's vision text by clustering line bounding boxes
     * spatially based on Y-coordinates (vertical alignment) and sorting left-to-right by X-coordinates.
     * This prevents ML Kit from breaking multi-column spreadsheet/receipt layouts into separated paragraphs.
     */
    fun reconstructSpatialText(visionText: Text, yTolerance: Int = 35): String {
        val textBlocks = visionText.textBlocks
        if (textBlocks.isEmpty()) return visionText.text

        val clusteredLines = mutableMapOf<Int, MutableList<Pair<Int, String>>>()

        for (block in textBlocks) {
            for (line in block.lines) {
                val boundingBox = line.boundingBox
                if (boundingBox != null) {
                    val centerY = boundingBox.top + (boundingBox.height() / 2)
                    val startX = boundingBox.left
                    val matchY = clusteredLines.keys.find { abs(it - centerY) <= yTolerance }

                    if (matchY != null) {
                        clusteredLines[matchY]!!.add(Pair(startX, line.text))
                    } else {
                        clusteredLines[centerY] = mutableListOf(Pair(startX, line.text))
                    }
                }
            }
        }

        val reconstructedTextBuilder = StringBuilder()
        val sortedClusters = clusteredLines.entries.sortedBy { it.key }
        for (entry in sortedClusters) {
            val sortedHorizontalLines = entry.value.sortedBy { it.first }.map { it.second }
            reconstructedTextBuilder.append(sortedHorizontalLines.joinToString(" ")).append("\n")
        }

        val resultText = reconstructedTextBuilder.toString()
        return if (resultText.isNotBlank()) resultText else visionText.text
    }

    /**
     * Requests native Android system deletion for the scanned screenshot URI if auto-delete is enabled.
     * Uses MediaStore.createDeleteRequest (Android 11+ / API 30+).
     */
    fun requestScreenshotDeletion(
        context: Context,
        uri: Uri,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        if (!isAutoDeleteEnabled(context)) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
                val intentSenderRequest = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                launcher.launch(intentSenderRequest)
            } catch (e: Exception) {
                Log.e("OcrHelper", "Failed to launch MediaStore.createDeleteRequest", e)
            }
        }
    }

    /**
     * Checks if auto-delete screenshot preference is enabled. Defaults to true.
     */
    fun isAutoDeleteEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_DELETE, true)
    }

    /**
     * Updates the auto-delete screenshot preference in SharedPreferences.
     */
    fun setAutoDeleteEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_AUTO_DELETE, enabled) }
    }
}
