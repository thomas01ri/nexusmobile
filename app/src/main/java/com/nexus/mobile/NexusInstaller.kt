package com.nexus.mobile

import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
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
    private const val DOWNLOAD_FOLDER = "Download/NEXUS"

    suspend fun sync(context: Context, onProgress: (InstallerProgress) -> Unit): InstallerResult =
        withContext(Dispatchers.IO) {
            val manifestConnection = URL(MANIFEST_URL).openConnection() as HttpURLConnection
            manifestConnection.connectTimeout = 15000
            manifestConnection.readTimeout = 20000
            manifestConnection.instanceFollowRedirects = true
            manifestConnection.setRequestProperty("User-Agent", "NEXUS-Mobile/1.3.0")

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
                        if (totalBytes > MAX_MANIFEST_BYTES) {
                            throw IOException("Manifest terlalu besar")
                        }
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

                if (eligible.isEmpty()) {
                    onProgress(InstallerProgress(100, "Folder data siap. Belum ada paket legal di manifest untuk diunduh."))
                    return@withContext InstallerResult(0, 0)
                }

                var completed = 0
                eligible.forEachIndexed { index, component ->
                    onProgress(
                        InstallerProgress(
                            (index * 100) / eligible.size,
                            "Memeriksa ${component.fileName}..."
                        )
                    )
                    if (isAlreadyPublished(context, component)) {
                        completed++
                        onProgress(InstallerProgress((completed * 100) / eligible.size, "Sudah tersedia: ${component.fileName}"))
                        return@forEachIndexed
                    }

                    val temp = File.createTempFile("nexus-", ".part", context.cacheDir)
                    try {
                        downloadAndVerify(component, temp) { percent ->
                            val combined = ((index * 100) + percent) / eligible.size
                            onProgress(
                                InstallerProgress(
                                    combined,
                                    "Mengunduh ${component.fileName}: $percent%"
                                )
                            )
                        }

                        publishToDownloads(context, component.fileName, temp)
                        completed++
                        onProgress(
                            InstallerProgress(
                                (completed * 100) / eligible.size,
                                "Tersimpan: $DOWNLOAD_FOLDER/${component.fileName}"
                            )
                        )
                    } finally {
                        temp.delete()
                    }
                }

                InstallerResult(completed, eligible.size)
            } finally {
                manifestConnection.disconnect()
            }
        }

    private data class Component(
        val fileName: String,
        val url: String,
        val sha256: String
    )

    private fun isAllowedUrl(value: String): Boolean {
        return value.startsWith(
            "https://github.com/thomas01ri/nexusmobile/releases/download/"
        ) || value.startsWith(
            "https://raw.githubusercontent.com/thomas01ri/nexusmobile/"
        )
    }

    private fun isAlreadyPublished(context: Context, component: Component): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
            resolver.query(
                collection,
                arrayOf(MediaStore.MediaColumns._ID),
                selection,
                arrayOf(component.fileName, "$DOWNLOAD_FOLDER/"),
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val uri = android.content.ContentUris.withAppendedId(collection, cursor.getLong(0))
                    val actualHash = resolver.openInputStream(uri)?.use { sha256(it) }
                    if (actualHash == component.sha256) return true
                }
            }
            return false
        }
        val base = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return false
        val file = File(File(base, "NEXUS"), component.fileName)
        return file.isFile && sha256(file) == component.sha256
    }

    private fun sha256(input: java.io.InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        input.use {
            val buffer = ByteArray(32 * 1024)
            while (true) {
                val count = it.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun downloadAndVerify(
        component: Component,
        target: File,
        onPercent: (Int) -> Unit
    ) {
        val connection = URL(component.url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 30000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "NEXUS-Mobile/1.3.0")

        try {
            connection.connect()
            if (connection.responseCode !in 200..299) {
                throw IOException("Download HTTP ${connection.responseCode}")
            }
            if (connection.url.host !in setOf(
                    "github.com",
                    "raw.githubusercontent.com",
                    "release-assets.githubusercontent.com"
                )
            ) {
                throw IOException("Host download tidak tepercaya")
            }

            val expectedLength = connection.contentLengthLong
            if (expectedLength > MAX_FILE_BYTES) {
                throw IOException("Komponen terlalu besar")
            }

            var received = 0L
            var lastPercent = -1
            FileOutputStream(target).use { output ->
                connection.inputStream.use { input ->
                    val buffer = ByteArray(32 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        received += count
                        if (received > MAX_FILE_BYTES) {
                            throw IOException("Ukuran komponen melewati batas")
                        }
                        output.write(buffer, 0, count)

                        val percent = if (expectedLength > 0) {
                            ((received * 100) / expectedLength).toInt().coerceIn(0, 100)
                        } else {
                            0
                        }
                        if (percent != lastPercent) {
                            lastPercent = percent
                            onPercent(percent)
                        }
                    }
                }
            }

            if (received == 0L) throw IOException("File hasil unduhan kosong")
            if (sha256(target) != component.sha256) {
                throw IOException("Verifikasi SHA-256 gagal untuk ${component.fileName}")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun publishToDownloads(context: Context, fileName: String, source: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            publishWithMediaStore(context, fileName, source)
        } else {
            // Fallback aman untuk Android 6-9: folder Downloads privat aplikasi.
            // Android 10+ menggunakan folder publik Download/NEXUS.
            val base = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: throw IOException("Folder Download aplikasi tidak tersedia")
            val directory = File(base, "NEXUS")
            if (!directory.exists() && !directory.mkdirs()) {
                throw IOException("Tidak bisa membuat folder Download/NEXUS")
            }
            val destination = File(directory, fileName)
            source.copyTo(destination, overwrite = true)
        }
    }

    private fun publishWithMediaStore(context: Context, fileName: String, source: File) {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val relativePath = "$DOWNLOAD_FOLDER/"

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val uri: Uri = resolver.insert(collection, values)
            ?: throw IOException("Tidak bisa membuat file di Download/NEXUS")

        try {
            resolver.openOutputStream(uri, "w")?.use { output ->
                source.inputStream().use { input ->
                    input.copyTo(output)
                }
            } ?: throw IOException("Tidak bisa menulis file ke Download/NEXUS")

            val publishValues = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            if (resolver.update(uri, publishValues, null, null) <= 0) {
                throw IOException("Tidak bisa menyelesaikan penyimpanan file")
            }

            // Hapus salinan lama dengan nama sama setelah file baru berhasil dipublikasikan.
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND " +
                "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND " +
                "${MediaStore.MediaColumns._ID} != ?"
            resolver.delete(
                collection,
                selection,
                arrayOf(fileName, relativePath, uri.lastPathSegment ?: "")
            )
        } catch (error: Exception) {
            resolver.delete(uri, null, null)
            throw error
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
