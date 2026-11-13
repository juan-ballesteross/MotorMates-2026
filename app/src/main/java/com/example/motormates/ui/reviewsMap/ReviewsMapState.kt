package com.example.motormates.ui.reviewsMap

import com.example.motormates.data.model.ReviewMapMarkerUi

/**
 * Lo que necesita el mapa de reseñas recientes.
 *
 * Del marcador abierto se guarda el id y no el objeto: el listener de
 * Firestore reemite con cada like o cada reseña nueva, y si se guardara la
 * copia quedaría desactualizada. Con el id, selectedMarker siempre resuelve
 * contra la lista vigente, y si la reseña desaparece la tarjeta se cierra
 * sola. Lo mismo vale para el grupo abierto (clusterSelectionIds).
 */
data class ReviewsMapUiState(
    val markers: List<ReviewMapMarkerUi> = emptyList(),
    val selectedMarkerId: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** Reseñas de un grupo que el zoom no puede separar (mismo punto). Vacío = hoja cerrada. */
    val clusterSelectionIds: List<String> = emptyList(),
    val locationAccess: LocationAccess = LocationAccess.NOT_REQUESTED,
    val isLocating: Boolean = false,
    /** A dónde mover la cámara por "Mi ubicación". Se consume una vez (consumeMyLocationTarget). */
    val myLocationTarget: MapCamera? = null,
    val locationMessage: String? = null,
    /**
     * La cámara se encuadra sola una única vez. Vive en el ViewModel y no en
     * la pantalla porque el mapa se destruye al cambiar de pestaña y el
     * ViewModel no: así, al volver, no salta otra vez.
     */
    val hasFittedInitially: Boolean = false,
    /** Marcadores que ya estaban en pantalla la última vez que se encuadró. */
    val acknowledgedMarkerIds: Set<String> = emptySet(),
    /** Última posición de la cámara, para restaurarla al volver a la pestaña. */
    val savedCamera: MapCamera? = null
) {
    val selectedMarker: ReviewMapMarkerUi?
        get() = markers.firstOrNull { it.id == selectedMarkerId }

    val clusterSelection: List<ReviewMapMarkerUi>
        get() = clusterSelectionIds.mapNotNull { id -> markers.firstOrNull { it.id == id } }

    /** Reseñas que llegaron después del último encuadre: alimenta el chip "N nuevas". */
    val newMarkersCount: Int
        get() = if (!hasFittedInitially) 0 else markers.count { it.id !in acknowledgedMarkerIds }

    /** Mapa vacío de verdad: ni cargando, ni con error, ni con marcadores. */
    val isEmpty: Boolean
        get() = markers.isEmpty() && !isLoading && errorMessage == null
}

/**
 * En qué punto está el permiso de ubicación. El mapa funciona en todos los
 * casos: la ubicación solo prende el punto azul y el botón "Mi ubicación".
 */
enum class LocationAccess {
    GRANTED,
    /** Nunca se ha pedido: al tocar "Mi ubicación" se explica y se pide. */
    NOT_REQUESTED,
    /** Lo negó, pero Android todavía deja volver a preguntar. */
    DENIED_CAN_ASK,
    /** "No volver a preguntar": la única salida es Ajustes. */
    DENIED_PERMANENTLY,
    /** Hay permiso pero la ubicación del teléfono está apagada. */
    SERVICES_OFF,
    /** Sin Google Play Services: no hay cómo ubicar, se oculta el botón. */
    UNAVAILABLE
}

/** Posición de la cámara sin tipos del SDK de Maps, para que el ViewModel se pruebe en la JVM. */
data class MapCamera(val lat: Double, val lng: Double, val zoom: Float)
