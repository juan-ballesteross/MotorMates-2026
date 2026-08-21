package com.example.motormates.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.motormates.ui.review.components.NewReviewTopBar
import com.example.motormates.ui.search.model.sampleSearchCars
import com.example.motormates.ui.theme.MotorMatesTheme

/**
 * Punto de entrada de "Nueva reseña". Guarda el estado del formulario
 * (calificación, texto, aspectos, fotos) y lo pasa a [NewReviewScreenContent].
 * Por ahora publicar solo cierra la pantalla; más adelante se conectará a API.
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
            .background(Color(0xFF000000))
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
        NewReviewScreen(car = sampleSearchCars.first().toReviewCarSummary())
    }
}
