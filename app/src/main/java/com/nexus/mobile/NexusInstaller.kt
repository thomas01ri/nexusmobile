package com.nexus.mobile

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class InstallerProgress(val percent: Int, val message: String)
data class InstallerResult(val completed: Int, val total: Int)

object GameInstallDetector {
    private const val GAME_PACKAGE = "com.rockstargames.gtasa"

    fun isInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getApplicationInfo(GAME_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }
}

object NexusInstaller {
    private const val MANIFEST_URL =
        "https://raw.githubusercontent.com/thomas01ri/nexusmobile/main/distributables/manifest.json"
    private const val MAX_FILE_BYTES = 256L * 1024L * 1024L
    private const val MAX_MANIFEST_BYTES = 1024L * 1024L

    suspend fun sync(context: Context, onProgress: (InstallerProgress) -> Unit): InstallerResult =
        withContext(Dispatchers.IO) {
            val manifestConnection = URL(MANIFEST_URL).openConnection() as HttpURLConnection
            manifestConnection.connectTimeout = 12000
            manifestConnection.readTimeout = 12000
            manifestConnection.setRequestProperty("User-Agent", "NEXUS-Mobile/1.2.0")
            try {
                manifestConnection.connect()
                if (manifestConnection.responseCode !in 200..299) {
                    throw IOException("Manifest HTTP ${manifestConnection.responseCode}")
                }
                val manifestBytes = manifestConnection.inputStream.use { input ->
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    var totalBytes = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        totalBytes += count
                        if (totalBytes > MAX_MANIFEST_BYTES) throw IOException("Manifest too large")
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                }
                val components = JSONObject(String(manifestBytes, Charsets.UTF_8))
                    .optJSONArray("components") ?: org.json.JSONArray()
                val eligible = (0 until components.length()).mapNotNull { index ->
                    val item = components.optJSONObject(index) ?: return@mapNotNull null
                    val redistributable = item.optBoolean("redistributable", false)
                    val license = item.optString("license").trim()
                    val name = item.optString("fileName").trim()
                    val url = item.optString("url").trim()
                    val hash = item.optString("sha256").lowercase()
                    if (!redistributable || license.isBlank()) return@mapNotNull null
                    if (!name.matches(Regex("[A-Za-z0-9._-]{1,100}"))) return@mapNotNull null
                    if (!hash.matches(Regex("[a-f0-9]{64}"))) return@mapNotNull null
                    if (!isAllowedUrl(url)) return@mapNotNull null
                    Component(name, url, hash)
                }
                val targetDirectory = File(context.filesDir, "nexus/components")
                if (!targetDirectory.exists() && !targetDirectory.mkdirs()) {
                    throw IOException("Cannot create component directory")
                }
                var completed = 0
                eligible.forEachIndexed { index, component ->
                    val target = File(targetDirectory, component.fileName)
                    val existingHash = if (target.isFile) sha256(target) else ""
                    if (existingHash == component.sha256) {
                        completed++
                        onProgress(InstallerProgress((completed * 100) / eligible.size, "Komponen terverifikasi: ${component.fileName}"))
                    } else {
                        onProgress(InstallerProgress((index * 100) / eligible.size, "Mengunduh ${component.fileName}..."))
                        downloadAndVerify(component, target) { percent ->
                            val combined = ((index * 100) + percent) / eligible.size
                            onProgress(InstallerProgress(combined, "Mengunduh ${component.fileName}: $percent%"))
                        }
                        completed++
                        onProgress(InstallerProgress((completed * 100) / eligible.size, "Terverifikasi: ${component.fileName}"))
                    }
                }
                InstallerResult(completed, eligible.size)
            } finally {
                manifestConnection.disconnect()
            }
        }

    private data class Component(val fileName: String, val url: String, val sha256: String)

    private fun isAllowedUrl(value: String): Boolean {
        return value.startsWith("https://github.com/thomas01ri/nexusmobile/releases/download/") ||
            value.startsWith("https://raw.githubusercontent.com/thomas01ri/nexusmobile/")
    }

    private fun downloadAndVerify(component: Component, target: File, onPercent: (Int) -> Unit) {
        val connection = URL(component.url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 20000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "NEXUS-Mobile/1.2.0")
        val temp = File(target.parentFile, target.name + ".part")
        try {
            connection.connect()
            if (connection.responseCode !in 200..299) throw IOException("Download HTTP ${connection.responseCode}")
            if (connection.url.host !in setOf("github.com", "raw.githubusercontent.com", "release-assets.githubusercontent.com")) {
                throw IOException("Untrusted download host")
            }
            val expectedLength = connection.contentLength.toLong()
            if (expectedLength > MAX_FILE_BYTES) throw IOException("Component too large")
            var received = 0L
            var lastPercent = -1
            FileOutputStream(temp).use { output ->
                connection.inputStream.use { input ->
                    val buffer = ByteArray(32 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        received += count
                        if (received > MAX_FILE_BYTES) throw IOException("Component exceeds size limit")
                        output.write(buffer, 0, count)
                        val percent = if (expectedLength > 0) {
                            ((received * 100) / expectedLength).toInt().coerceIn(0, 100)
                        } else 0
                        if (percent != lastPercent) {
                            lastPercent = percent
                            onPercent(percent)
                        }
                    }
                }
            }
            if (sha256(temp) != component.sha256) throw IOException("SHA-256 verification failed")
            if (target.exists() && !target.delete()) throw IOException("Cannot replace old component")
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
                temp.delete()
            }
        } catch (error: Exception) {
            temp.delete()
            throw error
        } finally {
            connection.disconnect()
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(32 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }
}
