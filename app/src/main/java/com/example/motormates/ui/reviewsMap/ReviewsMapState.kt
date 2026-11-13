package com.example.motormates.ui.reviewsMap

import com.example.motormates.data.model.ReviewMapMarkerUi

/**
 * Lo que necesita el mapa de reseñas recientes.
 *
 * Del marcador abierto se guarda el id y no el objeto: el listener de
 * Firestore reemite con cada like o cada reseña nueva, y si se guardara la
 * copia quedaría desactualizada. Con el id, selectedMarker siempre resuelve
 * contra la lista vigente, y si la reseña desaparece la tarjeta se cierra
 * sola.
 */
data class ReviewsMapUiState(
    val markers: List<ReviewMapMarkerUi> = emptyList(),
    val selectedMarkerId: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val selectedMarker: ReviewMapMarkerUi?
        get() = markers.firstOrNull { it.id == selectedMarkerId }

    /** Mapa vacío de verdad: ni cargando, ni con error, ni con marcadores. */
    val isEmpty: Boolean
        get() = markers.isEmpty() && !isLoading && errorMessage == null
}
