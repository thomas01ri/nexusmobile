package com.nexus.mobile

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

// NEXUS_V130_BUGFIX_APPLIED
object NexusStorage {
    private const val DATA_ROOT = "Download/NEXUS/Data"
    private const val MEDIA_GAME_FOLDER = "com.nexussamp.game"

    private val moduleNames = listOf("configs", "files", "CLEO", "AML", "MonetLoader", "ModLoader", "GameFiles")
    private val publicFolders = listOf(
        "Download/NEXUS", DATA_ROOT, "$DATA_ROOT/configs", "$DATA_ROOT/files",
        "$DATA_ROOT/mods", "$DATA_ROOT/mods/CLEO", "$DATA_ROOT/mods/AML",
        "$DATA_ROOT/mods/MonetLoader", "$DATA_ROOT/mods/ModLoader"
    )

    suspend fun prepareFolders(context: Context) = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            publicFolders.forEach { ensureMarker(context, it) }
        } else {
            val base = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: throw IOException("Folder Download aplikasi tidak tersedia")
            publicFolders.forEach { relative ->
                val directory = File(base, relative.removePrefix("Download/"))
                if (!directory.exists() && !directory.mkdirs()) throw IOException("Gagal membuat folder: $relative")
            }
        }
    }

    suspend fun prepareMonetLoaderMediaFolder(context: Context, treeUri: Uri): String = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IOException("Folder Android/media tidak bisa dibuka")
        if (!root.isDirectory || !root.canWrite()) throw IOException("Pilih folder Android/media dan izinkan akses tulis")
        val gameFolder = root.findFile(MEDIA_GAME_FOLDER) ?: root.createDirectory(MEDIA_GAME_FOLDER)
            ?: throw IOException("Gagal membuat folder $MEDIA_GAME_FOLDER")
        val monetFolder = gameFolder.findFile("MonetLoader") ?: gameFolder.createDirectory("MonetLoader")
            ?: throw IOException("Gagal membuat folder MonetLoader")
        if (!monetFolder.isDirectory) throw IOException("Tujuan MonetLoader bukan folder")
        "Android/media/$MEDIA_GAME_FOLDER/MonetLoader"
    }

    suspend fun importFile(context: Context, source: Uri, module: String, mediaTreeUri: String? = null): String = withContext(Dispatchers.IO) {
        require(moduleNames.contains(module)) { "Folder modul tidak dikenal: $module" }
        val name = resolveName(context, source)
        if (module == "MonetLoader" && !mediaTreeUri.isNullOrBlank()) {
            return@withContext importToMonetMedia(context, source, Uri.parse(mediaTreeUri), name)
        }

        val relativeFolder = when (module) {
            "configs" -> "$DATA_ROOT/configs"
            "files", "GameFiles" -> "$DATA_ROOT/files"
            else -> "$DATA_ROOT/mods/$module"
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val relativePath = "$relativeFolder/"
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, context.contentResolver.getType(source) ?: "application/octet-stream")
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val destination = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("Tidak bisa membuat file pada $relativeFolder")
            try {
                val output = context.contentResolver.openOutputStream(destination, "w")
                    ?: throw IOException("File tujuan tidak bisa dibuka")
                output.use { stream ->
                    val input = context.contentResolver.openInputStream(source)
                        ?: throw IOException("File sumber tidak bisa dibaca")
                    input.use { it.copyTo(stream) }
                }
                val publish = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                if (context.contentResolver.update(destination, publish, null, null) <= 0) {
                    throw IOException("File belum berhasil dipublikasikan")
                }
            } catch (error: Exception) {
                context.contentResolver.delete(destination, null, null)
                throw error
            }
            "$relativeFolder/$name"
        } else {
            val root = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: throw IOException("Folder Download aplikasi tidak tersedia")
            val directory = File(root, relativeFolder.removePrefix("Download/"))
            if (!directory.exists() && !directory.mkdirs()) throw IOException("Gagal membuat folder tujuan")
            val target = File(directory, name)
            val input = context.contentResolver.openInputStream(source) ?: throw IOException("File sumber tidak bisa dibaca")
            input.use { src -> target.outputStream().use { src.copyTo(it) } }
            target.absolutePath
        }
    }

    private suspend fun importToMonetMedia(context: Context, source: Uri, treeUri: Uri, name: String): String {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IOException("Izin Android/media sudah tidak tersedia")
        val gameFolder = root.findFile(MEDIA_GAME_FOLDER) ?: root.createDirectory(MEDIA_GAME_FOLDER)
            ?: throw IOException("Folder game tidak bisa dibuat")
        val monetFolder = gameFolder.findFile("MonetLoader") ?: gameFolder.createDirectory("MonetLoader")
            ?: throw IOException("Folder MonetLoader tidak bisa dibuat")
        monetFolder.findFile(name)?.delete()
        val mime = context.contentResolver.getType(source) ?: "application/octet-stream"
        val target = monetFolder.createFile(mime, name) ?: throw IOException("File MonetLoader tidak bisa dibuat")
        try {
            val input = context.contentResolver.openInputStream(source) ?: throw IOException("File sumber tidak bisa dibaca")
            val output = context.contentResolver.openOutputStream(target.uri, "w") ?: throw IOException("File tujuan tidak bisa dibuka")
            input.use { src -> output.use { src.copyTo(it) } }
        } catch (error: Exception) {
            target.delete()
            throw error
        }
        "Android/media/$MEDIA_GAME_FOLDER/MonetLoader/$name"
    }

    private fun ensureMarker(context: Context, folder: String) {
        val relativePath = "$folder/"
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
        resolver.query(collection, arrayOf(MediaStore.MediaColumns._ID), selection, arrayOf("README.txt", relativePath), null)
            ?.use { if (it.moveToFirst()) return }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "README.txt")
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values) ?: throw IOException("Tidak bisa membuat folder $folder")
        try {
            resolver.openOutputStream(uri, "w")?.bufferedWriter()?.use {
                it.write("Folder NEXUS. File di sini hanya disimpan, belum dimuat ke game.\n")
                it.write("Jalur folder: $folder\n")
            } ?: throw IOException("Tidak bisa menulis README folder")
            val publish = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            if (resolver.update(uri, publish, null, null) <= 0) throw IOException("Tidak bisa menyelesaikan folder $folder")
        } catch (error: Exception) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    private fun resolveName(context: Context, uri: Uri): String {
        var sourceName: String? = null
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) sourceName = it.getString(0)
        }
        val safe = (sourceName ?: uri.lastPathSegment ?: "imported_file")
            .substringAfterLast('/').replace(Regex("[^A-Za-z0-9._-]"), "_").take(100)
        return safe.ifBlank { "imported_file" }
    }
}
