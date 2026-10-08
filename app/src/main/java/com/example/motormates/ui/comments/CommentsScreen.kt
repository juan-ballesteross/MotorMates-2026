package com.example.motormates.ui.comments

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CommentsScreen(
    vehicleId: Int,
    onBackClick: () -> Unit = {},
    onAuthorClick: (Int) -> Unit = {},
    onEditReviewClick: (Int) -> Unit = {},
    reloadSignal: Boolean = false,
    onReloadHandled: () -> Unit = {},
    viewModel: CommentsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(vehicleId) {
        viewModel.load(vehicleId)
    }

    LaunchedEffect(reloadSignal) {
        if (reloadSignal) {
            onReloadHandled()
            viewModel.load(vehicleId)
        }
    }

    val pendingDeleteId = uiState.pendingDeleteReviewId
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

    CommentsScreenContent(
        reviews = uiState.reviews,
        isLoading = uiState.isLoading,
        errorMessage = uiState.errorMessage,
        onBackClick = onBackClick,
        onAuthorClick = onAuthorClick,
        onEditReviewClick = onEditReviewClick,
        onDeleteReviewClick = viewModel::askDeleteReview,
        modifier = modifier
    )
}
