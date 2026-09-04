package com.example.motormates.ui.review

import com.example.motormates.data.model.ReviewAspect

data class NewReviewUiState(
    val rating: Int = 4,
    val experience: String = "",
    val selectedAspects: Set<ReviewAspect> = setOf(ReviewAspect.COMFORT, ReviewAspect.PERFORMANCE),
    val photoCount: Int = 0
)
