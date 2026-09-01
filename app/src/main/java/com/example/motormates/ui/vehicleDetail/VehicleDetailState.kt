package com.example.motormates.ui.vehicleDetail

import com.example.motormates.data.model.CarDetailUi
import com.example.motormates.data.model.ReviewUi

/**
 * Estado combinado (no flows separados) porque vehicle y reviews se
 * buscan juntos a partir del mismo id — mismo criterio que usó el
 * profesor en TweetDetailState. vehicle es nulable porque la búsqueda
 * por id puede no encontrar nada.
 */
data class VehicleDetailState(
    val vehicle: CarDetailUi? = null,
    val reviews: List<ReviewUi> = emptyList()
)