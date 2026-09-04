package com.example.motormates.ui.user

import com.example.motormates.data.mock.UserMocks
import com.example.motormates.data.model.GarageCar
import com.example.motormates.data.model.ProfileTab
import com.example.motormates.data.model.UserProfile

data class UserUiState(
    val profile: UserProfile = UserMocks.sampleUserProfile,
    val cars: List<GarageCar> = UserMocks.sampleUserCars,
    val selectedTab: ProfileTab = ProfileTab.REVIEWS
)
