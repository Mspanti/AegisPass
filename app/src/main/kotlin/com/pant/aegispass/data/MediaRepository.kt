package com.pant.aegispass.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

/**
 * MediaRepository encapsulates all file operations needed by Private Safe features.
 * All heavy IO is performed on Dispatchers.IO.
 */
class MediaRepository(private val context: Context, private val contentResolver: ContentResolver) {

    data class GalleryItem(val fileName: String, val filePath: String, val uri: Uri)

    fun listPrivateSafeFiles(): Flow<List<GalleryItem>> = flow {
        // Very small implementation: scan a private app directory for files
        val dir = File(context.filesDir, "private_safe")
        if (!dir.exists()) dir.mkdirs()
        val items = dir.listFiles()?.map { f ->
            GalleryItem(f.name, f.absolutePath, Uri.fromFile(f))
        } ?: emptyList()
        emit(items)
    }.flowOn(Dispatchers.IO)

    suspend fun moveToPrivateSafe(sourceUri: Uri, targetFileName: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val dir = File(context.filesDir, "private_safe").apply { if (!exists()) mkdirs() }
            val targetFile = File(dir, targetFileName)
            contentResolver.openInputStream(sourceUri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deletePrivateFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val f = File(filePath)
            f.exists() && f.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // TODO: implement restoreToPublic
}
