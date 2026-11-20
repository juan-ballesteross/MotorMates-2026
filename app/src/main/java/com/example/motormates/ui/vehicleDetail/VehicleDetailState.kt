package com.example.motormates.ui.vehicleDetail

import com.example.motormates.data.model.Build
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
    val pendingDeleteReviewId: String? = null,
    /** Soy el dueño del vehículo (cambia el texto del botón de AI Tuning). */
    val isOwner: Boolean = false,
    /** Builds publicados por la comunidad para este vehículo. */
    val communityBuilds: List<Build> = emptyList()
)
