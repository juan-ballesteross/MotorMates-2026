package com.example.motormates.ui.publicProfile

import com.example.motormates.data.model.GarageCar
import com.example.motormates.data.model.ProfileTab
import com.example.motormates.data.model.UserProfile

data class PublicProfileUiState(
    val avatarResId: Int? = null,
    val profile: UserProfile? = null,
    val cars: List<GarageCar> = emptyList(),
    val selectedTab: ProfileTab = ProfileTab.REVIEWS
)
