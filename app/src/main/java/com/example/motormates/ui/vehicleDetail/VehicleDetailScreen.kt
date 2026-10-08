package com.example.motormates.ui.vehicleDetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.motormates.ui.vehicleDetail.components.VehicleDetailError

@Composable
fun VehicleDetailScreen(
    vehicleId: Int,
    onBackClick: () -> Unit = {},
    onWriteReviewClick: () -> Unit = {},
    onSeeAllReviewsClick: () -> Unit = {},
    onAuthorClick: (Int) -> Unit = {},
    onEditReviewClick: (Int) -> Unit = {},
    reloadSignal: Boolean = false,
    onReloadHandled: () -> Unit = {},
    viewModel: VehicleDetailViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(vehicleId) {
        viewModel.load(vehicleId)
    }

    // El padre levanta esta bandera cuando se crea o edita una reseña en
    // otra pantalla: LaunchedEffect(vehicleId) no vuelve a dispararse al
    // regresar porque la clave y la entrada del back stack son las mismas.
    LaunchedEffect(reloadSignal) {
        if (reloadSignal) {
            onReloadHandled()
            viewModel.load(vehicleId)
        }
    }

    val pendingDeleteId = state.pendingDeleteReviewId
    if (pendingDeleteId != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteReview,
            title = { Text(text = "Eliminar reseña") },
            text = { Text(text = "¿Seguro que quieres eliminar tu reseña? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDeleteReview(vehicleId) }) {
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

    val vehicle = state.vehicle
    when {
        state.isLoading && vehicle == null -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }

        vehicle == null -> VehicleDetailError(
            message = state.errorMessage ?: "Este vehículo no existe o fue eliminado.",
            onRetryClick = { viewModel.load(vehicleId) },
            onBackClick = onBackClick,
            modifier = modifier
        )

        else -> VehicleDetailContent(
            car = vehicle,
            reviews = state.reviews,
            isBookmarked = state.isBookmarked,
            onBackClick = onBackClick,
            onBookmarkClick = viewModel::bookmarkButtonPress,
            onWriteReviewClick = onWriteReviewClick,
            onSeeAllReviewsClick = onSeeAllReviewsClick,
            onAuthorClick = onAuthorClick,
            onEditReviewClick = onEditReviewClick,
            onDeleteReviewClick = viewModel::askDeleteReview,
            modifier = modifier
        )
    }
}
