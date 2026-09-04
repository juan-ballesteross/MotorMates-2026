package com.example.motormates.ui.user

import com.example.motormates.data.model.GarageCar
import com.example.motormates.data.model.ProfileTab
import com.example.motormates.data.model.UserProfile

data class UserUiState(
    val profile: UserProfile = UserProfile(
        name = "",
        handle = "",
        location = "",
        bio = "",
        reviewsCount = 0,
        followersDisplay = "0",
        followingCount = 0
    ),
    val cars: List<GarageCar> = emptyList(),
    val selectedTab: ProfileTab = ProfileTab.REVIEWS
)
