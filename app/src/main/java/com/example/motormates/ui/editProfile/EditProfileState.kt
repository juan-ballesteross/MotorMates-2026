package com.example.motormates.ui.editProfile

import com.example.motormates.data.model.GarageCar

data class EditProfileUiState(
    val username: String = "",
    val bio: String = "",
    val cars: List<GarageCar> = emptyList(),
    val profileImageUrl: String? = null,
    val isImageUploading: Boolean = false,
    val imageUploadError: String? = null,
    val coverImageUrl: String? = null,
    val isCoverUploading: Boolean = false,
    val coverUploadError: String? = null,
    val isSaving: Boolean = false,
    val saveError: String? = null
)
