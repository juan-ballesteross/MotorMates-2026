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
import com.example.motormates.data.seed.localAvatarResFor

@Composable
fun PublicProfileScreen(
    firebaseUid: String? = null,
    onBackClick: () -> Unit = {},
    onVehicleClick: (String) -> Unit = {},
    viewModel: PublicProfileViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(firebaseUid) {
        if (firebaseUid != null) {
            viewModel.loadProfileByUid(firebaseUid)
        }
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
            // Si el usuario no ha subido foto (avatarUrl/coverUrl), se usa una
            // local: los usuarios semilla tienen una fija, igual que en las
            // Historias y las reseñas, para no mostrar siempre la misma.
            coverResId = R.drawable.profile_cover,
            coverUrl = uiState.coverUrl,
            avatarResId = firebaseUid?.let { localAvatarResFor(it) } ?: R.drawable.user_3,
            avatarUrl = uiState.avatarUrl,
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
