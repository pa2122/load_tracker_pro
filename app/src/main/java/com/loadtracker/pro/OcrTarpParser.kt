package com.loadtracker.pro

import java.util.Locale

data class OcrTarpInfo(
    val tarpType: String?,
    val isPreTarped: Boolean?
)

object OcrTarpParser {
    private val tarpCodePattern =
        Regex("""\bTarp\s*[:=\-\s]*([SLNP])\b""", RegexOption.IGNORE_CASE)

    fun parse(text: String): OcrTarpInfo? {
        val code = tarpCodePattern.find(text)
            ?.groupValues
            ?.get(1)
            ?.uppercase(Locale.US)
            ?: return null

        return when (code) {
            "S" -> OcrTarpInfo(tarpType = "4' Drop", isPreTarped = null)
            "L" -> OcrTarpInfo(tarpType = "8' Drop", isPreTarped = null)
            "N" -> OcrTarpInfo(tarpType = "None", isPreTarped = false)
            "P" -> OcrTarpInfo(tarpType = null, isPreTarped = true)
            else -> null
        }
    }
}
