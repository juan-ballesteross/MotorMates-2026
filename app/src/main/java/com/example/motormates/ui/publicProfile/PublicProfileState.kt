package com.example.motormates.ui.publicProfile

import com.example.motormates.data.model.ProfileTab
import com.example.motormates.data.model.UserProfile
import com.example.motormates.data.model.UserReviewUi

data class PublicProfileUiState(
    val profile: UserProfile? = null,
    val reviews: List<UserReviewUi> = emptyList(),
    val selectedTab: ProfileTab = ProfileTab.REVIEWS,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    // Solo se llena para usuarios reales de Firebase (ver loadProfileByUid).
    val avatarUrl: String? = null,
    val coverUrl: String? = null,
    val isFollowedByMe: Boolean = false
)
