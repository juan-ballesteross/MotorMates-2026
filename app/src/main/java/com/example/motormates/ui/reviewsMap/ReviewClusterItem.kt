package com.example.motormates.ui.reviewsMap

import com.example.motormates.data.model.ReviewMapMarkerUi
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

/**
 * Un punto del mapa: una reseña sola o un grupo de reseñas a 30 m o menos
 * (ver groupNearby). Los grupos cercanos se arman antes de pasárselos a la
 * librería porque ella solo agrupa por distancia en pantalla y a partir de 4
 * marcadores: dos reseñas en la misma cuadra se quedaban montadas una encima
 * de la otra al acercar.
 *
 * Vive en la capa de UI para que ReviewMapMarkerUi no dependa del SDK de
 * Maps (lo consumen pruebas de JVM puro). Es data class a propósito: cuando
 * cambia la selección o llega una foto, el ítem deja de ser igual al
 * anterior y Clustering vuelve a dibujarlo; si fuera el mismo objeto, el
 * bitmap ya rasterizado se quedaría con el ícono de respaldo.
 */
data class ReviewClusterItem(
    /** Nunca vacío. La primera es la más reciente y es la "cara" del punto. */
    val markers: List<ReviewMapMarkerUi>,
    val isSelected: Boolean,
    /** Cuántas fotos del grupo ya llegaron: va en el ítem solo para forzar el redibujo. */
    val loadedPhotos: Int,
    /** Va en el ítem solo para que cambiar de paleta lo redibuje (ver loadedPhotos). */
    val isDarkMap: Boolean = true
) : ClusterItem {

    val marker: ReviewMapMarkerUi get() = markers.first()

    val size: Int get() = markers.size

    val ids: List<String> get() = markers.map { it.id }

    /** El grupo se dibuja en el centro de sus reseñas. */
    private val latLng = LatLng(markers.map { it.lat }.average(), markers.map { it.lng }.average())

    override fun getPosition(): LatLng = latLng

    /** Título y snippet son lo que lee TalkBack sobre el marcador. */
    override fun getTitle(): String =
        if (size == 1) marker.vehicleName else "$size reseñas en este lugar"

    override fun getSnippet(): String =
        if (size == 1) {
            "${marker.calificacion} de 5 estrellas · ${marker.nombreUsuario} · ${marker.tiempoTexto}"
        } else {
            markers.joinToString(" · ") { it.vehicleName }
        }

    /** El seleccionado queda por encima de sus vecinos. */
    override fun getZIndex(): Float = if (isSelected) 1f else 0f
}
