package com.nexus.mobile

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class NexusUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val notes: String,
    val downloadUrl: String
)

object NexusUpdateChecker {
    private const val UPDATE_URL =
        "https://raw.githubusercontent.com/thomas01ri/nexusmobile/main/update.json"

    @Suppress("DEPRECATION")
    suspend fun check(context: Context): NexusUpdateInfo? = withContext(Dispatchers.IO) {
        val connection = URL(UPDATE_URL).openConnection() as HttpURLConnection
        connection.connectTimeout = 8000
        connection.readTimeout = 10000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "NEXUS-Mobile")
        try {
            connection.connect()
            if (connection.responseCode !in 200..299) throw IOException("Update check HTTP ${connection.responseCode}")
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val latestCode = json.optInt("versionCode", 0)
            val latestName = json.optString("versionName", "").trim()
            val notes = json.optString("notes", "Pembaruan NEXUS tersedia.")
            val url = json.optString("downloadUrl", "").trim()
            val currentCode = context.packageManager.getPackageInfo(context.packageName, 0).versionCode
            if (latestCode <= currentCode || latestName.isBlank()) return@withContext null
            if (!url.startsWith("https://github.com/")) return@withContext null
            NexusUpdateInfo(latestCode, latestName, notes, url)
        } finally {
            connection.disconnect()
        }
    }
}
