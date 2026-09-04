package com.example.motormates.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.motormates.data.mock.SearchMocks
import com.example.motormates.data.model.ReviewAspect
import com.example.motormates.data.model.ReviewCarSummary
import com.example.motormates.data.model.toReviewCarSummary
import com.example.motormates.ui.review.components.NewReviewTopBar
import com.example.motormates.ui.theme.MotorMatesTheme

/**
 * Punto de entrada de "Nueva reseña". Guarda el estado del formulario
 * (calificación, texto, aspectos, fotos); el auto reseñado lo resuelve
 * quien navega hasta acá (ver AppNavigation), a partir del id del
 * vehículo actual. Por ahora publicar solo cierra la pantalla.
 */
@Composable
fun NewReviewScreen(
    car: ReviewCarSummary,
    onCloseClick: () -> Unit = {},
    onPublishClick: (
        rating: Int,
        experience: String,
        aspects: Set<ReviewAspect>,
        photoCount: Int
    ) -> Unit = { _, _, _, _ -> },
    viewModel: NewReviewViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val canPublish = uiState.rating > 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        NewReviewTopBar(onCloseClick = onCloseClick)
        NewReviewScreenContent(
            car = car,
            rating = uiState.rating,
            onRatingChange = viewModel::updateRating,
            experience = uiState.experience,
            onExperienceChange = viewModel::updateExperience,
            selectedAspects = uiState.selectedAspects,
            onToggleAspect = { aspect ->
                viewModel.toggleAspect(aspect)
            },
            photoCount = uiState.photoCount,
            // Stub local: cada toque suma una "foto" hasta 3 (sin picker real aún).
            onAddPhotoClick = viewModel::addPhotoButtonPress,
            canPublish = canPublish,
            onPublishClick = {
                onPublishClick(
                    uiState.rating,
                    uiState.experience,
                    uiState.selectedAspects,
                    uiState.photoCount
                )
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NewReviewScreenPreview() {
    MotorMatesTheme {
        NewReviewScreen(car = SearchMocks.sampleSearchCars.first().toReviewCarSummary())
    }
}
