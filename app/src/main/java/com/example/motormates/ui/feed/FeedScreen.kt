package com.example.motormates.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.motormates.ui.feed.components.FeedTopBar

@Composable
fun FeedScreen(
    onVehicleClick: (Int) -> Unit = {},
    onStoryClick: (Int) -> Unit = {},
    viewModel: FeedViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        FeedTopBar()
        FeedScreenContent(
            stories = uiState.stories,
            vehicles = uiState.vehicles,
            isLoading = uiState.isLoading,
            errorMessage = uiState.errorMessage,
            onVehicleClick = onVehicleClick,
            onStoryClick = onStoryClick,
            onRetryClick = viewModel::loadVehicles
        )
    }
}
