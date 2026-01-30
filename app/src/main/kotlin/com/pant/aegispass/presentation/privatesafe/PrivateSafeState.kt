package com.pant.aegispass.presentation.privatesafe

import com.pant.aegispass.data.MediaRepository

data class PrivateSafeState(
    val isLoading: Boolean = false,
    val items: List<MediaRepository.GalleryItem> = emptyList(),
    val error: String? = null
)