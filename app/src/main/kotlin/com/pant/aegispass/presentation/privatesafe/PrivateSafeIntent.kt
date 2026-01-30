package com.pant.aegispass.presentation.privatesafe

import android.net.Uri

sealed interface PrivateSafeIntent {
    object LoadGallery : PrivateSafeIntent
    data class MoveToPrivate(val sourceUri: Uri, val fileName: String) : PrivateSafeIntent
    data class DeletePrivate(val filePath: String) : PrivateSafeIntent
}