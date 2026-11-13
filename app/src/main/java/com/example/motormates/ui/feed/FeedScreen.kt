package com.example.motormates.ui.feed

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.motormates.ui.feed.components.FeedTabsRow
import com.example.motormates.ui.feed.components.FeedTopBar
import com.example.motormates.ui.review.components.PhotoSourceDialog
import java.io.File

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
    val context = LocalContext.current
    var cameraPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }

    val storyPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let(viewModel::postStory)
    }

    val storyCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success -> if (success) cameraPhotoUri?.let(viewModel::postStory) }

    val storyCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createStoryPhotoUri(context)
            cameraPhotoUri = uri
            storyCameraLauncher.launch(uri)
        }
    }

    if (showPhotoSourceDialog) {
        PhotoSourceDialog(
            onDismiss = { showPhotoSourceDialog = false },
            onCameraClick = {
                showPhotoSourceDialog = false
                storyCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            },
            onGalleryClick = {
                showPhotoSourceDialog = false
                storyPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        )
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
            onAddStoryClick = { showPhotoSourceDialog = true },
            onDismissStoryError = viewModel::dismissStoryError,
            onRetryClick = viewModel::loadVehicles,
            onFollowingRetryClick = viewModel::retryFollowingReviews,
            onAuthorClick = onAuthorClick,
            onLikeClick = viewModel::toggleLike
        )
    }
}

private fun createStoryPhotoUri(context: Context): Uri {
    val photosDir = File(context.cacheDir, "story_photos").apply { mkdirs() }
    val file = File(photosDir, "IMG_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
