package com.example.motormates.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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
    modifier: Modifier = Modifier
) {
    var rating by remember { mutableIntStateOf(4) }
    var experience by remember { mutableStateOf("") }
    var selectedAspects by remember {
        mutableStateOf(setOf(ReviewAspect.COMFORT, ReviewAspect.PERFORMANCE))
    }
    var photoCount by remember { mutableIntStateOf(0) }

    val canPublish = rating > 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        NewReviewTopBar(onCloseClick = onCloseClick)
        NewReviewScreenContent(
            car = car,
            rating = rating,
            onRatingChange = { rating = it },
            experience = experience,
            onExperienceChange = { experience = it },
            selectedAspects = selectedAspects,
            onToggleAspect = { aspect ->
                selectedAspects = if (aspect in selectedAspects) {
                    selectedAspects - aspect
                } else {
                    selectedAspects + aspect
                }
            },
            photoCount = photoCount,
            // Stub local: cada toque suma una "foto" hasta 3 (sin picker real aún).
            onAddPhotoClick = { if (photoCount < 3) photoCount += 1 },
            canPublish = canPublish,
            onPublishClick = {
                onPublishClick(rating, experience, selectedAspects, photoCount)
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
