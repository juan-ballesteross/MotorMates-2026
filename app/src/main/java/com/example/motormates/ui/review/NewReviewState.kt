package com.example.motormates.ui.review

import android.net.Uri
import com.example.motormates.data.model.ReviewAspect
import com.example.motormates.data.model.ReviewCarSummary
import com.example.motormates.data.model.ReviewLocation

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
    val reviewId: String? = null,
    val rating: Int = 0,
    val experience: String = "",
    val selectedAspects: Set<ReviewAspect> = emptySet(),
    val existingImageUrls: List<String> = emptyList(),
    val newPhotoUris: List<Uri> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isSaved: Boolean = false,
    /**
     * La ubicación que se guardará con la reseña, ya resuelta ANTES de
     * publicar: así el usuario ve qué se va a adjuntar en vez de que se
     * capture en silencio al tocar el botón. null = no se adjunta ninguna.
     */
    val capturedLocation: ReviewLocation? = null,
    val isLocationLoading: Boolean = false,
    /**
     * Se intentó y no se pudo (permiso negado, sin GPS, sin señal). Es un
     * estado normal, no un error: la reseña se publica igual sin ubicación.
     */
    val isLocationUnavailable: Boolean = false
)
