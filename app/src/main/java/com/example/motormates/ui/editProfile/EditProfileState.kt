package com.example.motormates.ui.editProfile

import com.example.motormates.data.model.GarageCar

data class EditProfileUiState(
    val username: String = "",
    val bio: String = "",
    val cars: List<GarageCar> = emptyList()
)
