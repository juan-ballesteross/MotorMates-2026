package com.example.motormates.ui.vehicleDetail

import com.example.motormates.data.model.CarDetailUi
import com.example.motormates.data.model.ReviewUi

/**
 * Estado combinado (no flows separados) porque vehicle y reviews se
 * consultan juntos a partir del mismo id. vehicle es nulable porque el
 * backend puede responder 404.
 */
data class VehicleDetailState(
    val vehicle: CarDetailUi? = null,
    val reviews: List<ReviewUi> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isBookmarked: Boolean = false,
    val pendingDeleteReviewId: Int? = null
)
