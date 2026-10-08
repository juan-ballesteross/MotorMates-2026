package com.example.motormates.ui.publicProfile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.motormates.R

@Composable
fun PublicProfileScreen(
    userId: Int,
    onBackClick: () -> Unit = {},
    onVehicleClick: (Int) -> Unit = {},
    viewModel: PublicProfileViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(userId) {
        viewModel.loadProfile(userId)
    }

    val profile = uiState.profile
    when {
        profile == null && uiState.isLoading -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }

        profile == null -> Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = uiState.errorMessage ?: "No se pudo cargar el perfil",
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }

        else -> PublicProfileScreenContent(
            // Decoración: el backend no guarda portada ni avatar de usuario.
            coverResId = R.drawable.profile_cover,
            avatarResId = R.drawable.user_3,
            profile = profile,
            reviews = uiState.reviews,
            isLoading = uiState.isLoading,
            errorMessage = uiState.errorMessage,
            selectedTab = uiState.selectedTab,
            onSelectTab = viewModel::updateSelectedTab,
            onBackClick = onBackClick,
            onFollowClick = {},
            onMessageClick = {},
            onReviewClick = onVehicleClick,
            modifier = modifier
        )
    }
}
