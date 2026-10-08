package com.example.motormates.ui.feed

import com.example.motormates.data.model.FeedVehicleUi

data class FeedUiState(
    val vehicles: List<FeedVehicleUi> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
