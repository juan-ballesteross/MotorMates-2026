package com.example.motormates.ui.feed

import com.example.motormates.data.model.FeedVehicleUi
import com.example.motormates.data.model.StoryUser

data class FeedUiState(
    val stories: List<StoryUser> = emptyList(),
    val vehicles: List<FeedVehicleUi> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
