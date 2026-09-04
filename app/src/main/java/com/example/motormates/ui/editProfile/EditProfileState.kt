package com.example.motormates.ui.editProfile

import com.example.motormates.data.mock.UserMocks
import com.example.motormates.data.model.GarageCar

data class EditProfileUiState(
    val username: String = UserMocks.sampleUserProfile.name,
    val bio: String = UserMocks.sampleUserProfile.bio,
    val cars: List<GarageCar> = UserMocks.sampleUserCars
)
