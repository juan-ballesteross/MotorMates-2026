package com.example.motormates.ui.pickLocation

import com.example.motormates.ui.reviewsMap.LocationAccess
import com.example.motormates.ui.reviewsMap.MapCamera

/**
 * Lo que necesita la pantalla de elegir un punto en el mapa.
 *
 * El punto elegido NO vive aquí: es siempre el centro de la cámara, y la
 * cámara la maneja el propio mapa. Guardarlo además en el estado sería tener
 * la misma verdad en dos sitios.
 */
data class PickLocationUiState(
    /** Dónde abrir el mapa: el punto que ya tenía el evento, si venía uno. */
    val initialCamera: MapCamera? = null,
    val locationAccess: LocationAccess = LocationAccess.NOT_REQUESTED,
    val isLocating: Boolean = false,
    /** A dónde mover la cámara por "Mi ubicación". Se consume una vez. */
    val myLocationTarget: MapCamera? = null,
    val locationMessage: String? = null,
    /** Se resuelve al confirmar, para prellenar el nombre del sitio. */
    val isResolvingPlace: Boolean = false
)
