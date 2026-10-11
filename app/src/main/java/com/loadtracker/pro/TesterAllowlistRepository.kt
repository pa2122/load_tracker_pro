package com.loadtracker.pro

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object TesterAllowlistRepository {
    private const val ALLOWLIST_URL =
        "https://raw.githubusercontent.com/pa2122/load_tracker_pro/master/authorized_devs.json"

    suspend fun fetch(): Set<String> = withContext(Dispatchers.IO) {
        val connection = URL(ALLOWLIST_URL).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Tester allowlist request failed with HTTP $responseCode")
            }

            val json = connection.inputStream.bufferedReader().use { it.readText() }
            parse(json)
        } finally {
            connection.disconnect()
        }
    }

    internal fun parse(json: String): Set<String> {
        val array = JSONArray(json)
        return buildSet {
            for (index in 0 until array.length()) {
                val value = array.get(index)
                if (value !is String) {
                    throw JSONException("Tester allowlist entries must be strings")
                }
                value.trim().takeIf(String::isNotEmpty)?.let(::add)
            }
        }
    }
}
