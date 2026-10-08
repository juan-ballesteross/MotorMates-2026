package com.example.motormates.ui.publicProfile

import com.example.motormates.data.model.ProfileTab
import com.example.motormates.data.model.UserProfile
import com.example.motormates.data.model.UserReviewUi

data class PublicProfileUiState(
    val profile: UserProfile? = null,
    val reviews: List<UserReviewUi> = emptyList(),
    val selectedTab: ProfileTab = ProfileTab.REVIEWS,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
