package com.example.motormates.ui.user

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.motormates.ui.common.components.MainBottomDestination
import com.example.motormates.ui.common.components.MainBottomNavBar
import com.example.motormates.ui.theme.MotorMatesTheme

/**
 * Ya NO tiene su propio Scaffold — el único Scaffold de la app vive en
 * MainActivity.kt (MotorMatesApp), igual que Feed y Search.
 */
@Composable
fun UserScreen(
    onEditProfileClick: () -> Unit = {},
    onVehicleClick: (Int) -> Unit = {},
    onEditReviewClick: (Int, Int) -> Unit = { _, _ -> },
    reloadSignal: Boolean = false,
    onReloadHandled: () -> Unit = {},
    viewModel: UserViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Igual que en VehicleDetailScreen: al volver de editar una reseña el
    // LaunchedEffect de carga inicial no se vuelve a disparar solo, así que
    // el padre levanta esta bandera para forzar la recarga.
    LaunchedEffect(reloadSignal) {
        if (reloadSignal) {
            onReloadHandled()
            viewModel.load()
        }
    }

    val pendingDeleteId = uiState.pendingDeleteReviewId
    if (pendingDeleteId != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteReview,
            title = { Text(text = "Eliminar reseña") },
            text = { Text(text = "¿Seguro que quieres eliminar tu reseña? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDeleteReview) {
                    Text(text = "Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteReview) {
                    Text(text = "Cancelar")
                }
            }
        )
    }

    UserScreenContent(
        profile = uiState.profile,
        profileImageUrl = uiState.profileImageUrl,
        reviews = uiState.reviews,
        isLoading = uiState.isLoading,
        errorMessage = uiState.errorMessage,
        cars = uiState.cars,
        selectedTab = uiState.selectedTab,
        onSelectTab = viewModel::updateSelectedTab,
        onEditProfileClick = onEditProfileClick,
        onReviewClick = onVehicleClick,
        onEditReviewClick = { reviewId ->
            uiState.reviews.find { it.id == reviewId }?.let { review ->
                onEditReviewClick(review.vehicleId, reviewId)
            }
        },
        onDeleteReviewClick = viewModel::askDeleteReview,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun UserScreenPreview() {
    MotorMatesTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = { MainBottomNavBar(selected = MainBottomDestination.PROFILE) }
        ) { innerPadding ->
            UserScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}
