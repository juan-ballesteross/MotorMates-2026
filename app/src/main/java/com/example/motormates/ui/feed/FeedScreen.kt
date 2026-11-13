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
import com.example.motormates.data.model.FeedTab
import com.example.motormates.ui.feed.components.FeedTabsRow
import com.example.motormates.ui.feed.components.FeedTopBar
import com.example.motormates.ui.reviewsMap.ReviewsMapScreen

@Composable
fun FeedScreen(
    onVehicleClick: (String) -> Unit = {},
    onStoryClick: (Int) -> Unit = {},
    onAuthorClick: (String) -> Unit = {},
    onAlertsClick: () -> Unit = {},
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
        FeedTopBar(onAlertsClick = onAlertsClick)
        FeedTabsRow(
            selectedTab = uiState.selectedTab,
            onTabClick = viewModel::updateSelectedTab
        )

        // El mapa se compone acá y no dentro de FeedScreenContent por dos
        // razones: un GoogleMap no puede ir dentro de un LazyColumn (recibe
        // alto infinito), y FeedScreenContent es stateless — meterle un
        // hiltViewModel() adentro le quitaría esa propiedad y rompería sus
        // previews. Al vivir en la rama, el mapa solo se crea cuando la
        // pestaña está activa y se destruye al salir, liberando la MapView.
        if (uiState.selectedTab == FeedTab.MAPA) {
            ReviewsMapScreen(onVehicleClick = onVehicleClick, onAuthorClick = onAuthorClick)
            return@Column
        }

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
