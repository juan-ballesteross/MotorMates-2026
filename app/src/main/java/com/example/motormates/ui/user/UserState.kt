package com.example.motormates.ui.user

import com.example.motormates.data.model.ActivityUi
import com.example.motormates.data.model.GarageVehicleUi
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
    // Los vehículos que publiqué (ownerId = mi uid).
    val cars: List<GarageVehicleUi> = emptyList(),
    val selectedTab: ProfileTab = ProfileTab.REVIEWS,
    val profileImageUrl: String? = null,
    val coverImageUrl: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val pendingDeleteReviewId: String? = null,
    // Pestaña Actividad: se carga al abrirla.
    val activity: List<ActivityUi> = emptyList(),
    val isActivityLoading: Boolean = false,
    val activityError: String? = null
)
