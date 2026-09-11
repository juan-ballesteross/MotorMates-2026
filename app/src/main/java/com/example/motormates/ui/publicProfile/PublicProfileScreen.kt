package com.example.motormates.ui.publicProfile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.motormates.R
import com.example.motormates.ui.theme.MotorMatesTheme

@Composable
fun PublicProfileScreen(
    storyIndex: Int,
    onBackClick: () -> Unit = {},
    viewModel: PublicProfileViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(storyIndex) {
        viewModel.loadProfile(storyIndex)
    }

    val profile = uiState.profile
    val avatarResId = uiState.avatarResId
    if (profile != null && avatarResId != null) {
        PublicProfileScreenContent(
            coverResId = R.drawable.sofia_profile_cover,
            avatarResId = avatarResId,
            profile = profile,
            cars = uiState.cars,
            selectedTab = uiState.selectedTab,
            onSelectTab = viewModel::updateSelectedTab,
            onBackClick = onBackClick,
            onFollowClick = {},
            onMessageClick = {},
            modifier = modifier
        )
    } else {
        Box(modifier = modifier.fillMaxSize())
    }
}

@Preview(showBackground = true)
@Composable
private fun PublicProfileScreenPreview() {
    MotorMatesTheme {
        PublicProfileScreen(storyIndex = 4)
    }
}
