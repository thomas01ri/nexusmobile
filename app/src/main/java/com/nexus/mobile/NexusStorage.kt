package com.nexus.mobile

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

object NexusStorage {
    private val moduleNames = listOf("CLEO", "AML", "MonetLoader", "ModLoader", "GameFiles")

    suspend fun prepareFolders(context: Context) = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ensureMarker(context, "Download/NEXUS")
            ensureMarker(context, "Download/NEXUS/Data")
            moduleNames.forEach { ensureMarker(context, "Download/NEXUS/Data/$it") }
        } else {
            val base = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: throw IOException("Folder Download aplikasi tidak tersedia")
            val data = File(base, "NEXUS/Data")
            listOf(data, *moduleNames.map { File(data, it) }.toTypedArray()).forEach {
                if (!it.exists() && !it.mkdirs()) throw IOException("Gagal membuat folder: ${it.name}")
            }
        }
    }

    suspend fun importFile(context: Context, source: Uri, module: String): String =
        withContext(Dispatchers.IO) {
            require(moduleNames.contains(module)) { "Folder modul tidak dikenal" }
            val name = resolveName(context, source)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val relativePath = "Download/NEXUS/Data/$module/"
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, context.contentResolver.getType(source) ?: "application/octet-stream")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val destination = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw IOException("Folder Download/NEXUS/Data tidak bisa ditulis")
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
                "Download/NEXUS/Data/$module/$name"
            } else {
                val root = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: throw IOException("Folder Download aplikasi tidak tersedia")
                val directory = File(root, "NEXUS/Data/$module")
                if (!directory.exists() && !directory.mkdirs()) throw IOException("Gagal membuat folder modul")
                val target = File(directory, name)
                val input = context.contentResolver.openInputStream(source)
                    ?: throw IOException("File sumber tidak bisa dibaca")
                input.use { sourceStream -> target.outputStream().use { sourceStream.copyTo(it) } }
                target.absolutePath
            }
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
                it.write("Folder NEXUS untuk menyimpan file secara manual.\n")
                it.write("NEXUS belum menjalankan atau memasang loader ke game.\n")
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
            .substringAfterLast('/')
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .take(100)
        return safe.ifBlank { "imported_file" }
    }
}
