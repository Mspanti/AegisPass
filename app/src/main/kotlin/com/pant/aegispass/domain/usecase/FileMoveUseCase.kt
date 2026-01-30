package com.pant.aegispass.domain.usecase

import com.pant.aegispass.data.MediaRepository
import kotlinx.coroutines.flow.Flow

class FileMoveUseCase(private val mediaRepository: MediaRepository) {
    fun listPrivateFiles(): Flow<List<MediaRepository.GalleryItem>> = mediaRepository.listPrivateSafeFiles()
    suspend fun moveToPrivate(sourceUri: android.net.Uri, fileName: String): Boolean = mediaRepository.moveToPrivateSafe(sourceUri, fileName)
    suspend fun deletePrivate(filePath: String): Boolean = mediaRepository.deletePrivateFile(filePath)
}