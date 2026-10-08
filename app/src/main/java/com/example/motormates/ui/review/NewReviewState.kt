package com.example.motormates.ui.review

import com.example.motormates.data.model.ReviewAspect
import com.example.motormates.data.model.ReviewCarSummary

/**
 * reviewId distingue los dos modos de la pantalla: null = crear,
 * no null = editar una reseña existente.
 *
 * selectedAspects y photoCount son estado puramente visual: el backend
 * solo guarda rating y comment en la tabla reviews, y el PUT solo acepta
 * esos dos campos, así que no hay dónde persistirlos. No se envían a
 * propósito — meterlos dentro del comment lo corrompería en cada edición.
 */
data class NewReviewUiState(
    val car: ReviewCarSummary? = null,
    val reviewId: Int? = null,
    val rating: Int = 0,
    val experience: String = "",
    val selectedAspects: Set<ReviewAspect> = emptySet(),
    val photoCount: Int = 0,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isSaved: Boolean = false
)
