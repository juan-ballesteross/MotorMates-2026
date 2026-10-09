package com.example.motormates.ui.user

import com.example.motormates.data.model.GarageCar
import com.example.motormates.data.model.ProfileTab
import com.example.motormates.data.model.UserProfile
import com.example.motormates.data.model.UserReviewUi

data class UserUiState(
    val profile: UserProfile = UserProfile(
        name = "",
        handle = "",
        location = "",
        bio = "",
        reviewsCount = 0,
        followersDisplay = "0",
        followingDisplay = "0"
    ),
    val reviews: List<UserReviewUi> = emptyList(),
    // El garaje sigue siendo local: el backend no tiene ese concepto.
    val cars: List<GarageCar> = emptyList(),
    val selectedTab: ProfileTab = ProfileTab.REVIEWS,
    val profileImageUrl: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val pendingDeleteReviewId: Int? = null
)
