package com.example.motormates.ui.feed

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.motormates.ui.feed.components.FeedTabsRow
import com.example.motormates.ui.feed.components.FeedTopBar

@Composable
fun FeedScreen(
    onVehicleClick: (String) -> Unit = {},
    onStoryClick: (Int) -> Unit = {},
    onAuthorClick: (String) -> Unit = {},
    viewModel: FeedViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val storyPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let(viewModel::postStory)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        FeedTopBar()
        FeedTabsRow(
            selectedTab = uiState.selectedTab,
            onTabClick = viewModel::updateSelectedTab
        )
        FeedScreenContent(
            selectedTab = uiState.selectedTab,
            stories = uiState.stories,
            vehicles = uiState.vehicles,
            isLoading = uiState.isLoading,
            errorMessage = uiState.errorMessage,
            followingReviews = uiState.followingReviews,
            isFollowingLoading = uiState.isFollowingLoading,
            followingErrorMessage = uiState.followingErrorMessage,
            storyErrorMessage = uiState.storyErrorMessage,
            onVehicleClick = onVehicleClick,
            onStoryClick = onStoryClick,
            onAddStoryClick = {
                storyPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onDismissStoryError = viewModel::dismissStoryError,
            onRetryClick = viewModel::loadVehicles,
            onFollowingRetryClick = viewModel::retryFollowingReviews,
            onAuthorClick = onAuthorClick,
            onLikeClick = viewModel::toggleLike
        )
    }
}
