package com.example.motormates.ui.reviewsMap

import com.example.motormates.data.model.ReviewMapMarkerUi
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

/**
 * Adaptador entre el marcador de la app y la librería de clusters.
 *
 * Vive en la capa de UI para que ReviewMapMarkerUi no dependa del SDK de
 * Maps (lo consumen pruebas de JVM puro). Es data class a propósito: cuando
 * cambia la selección o llega la foto, el ítem deja de ser igual al anterior
 * y Clustering vuelve a dibujar ese marcador; si fuera el mismo objeto, el
 * bitmap ya rasterizado se quedaría con el ícono de respaldo.
 */
data class ReviewClusterItem(
    val marker: ReviewMapMarkerUi,
    val isSelected: Boolean,
    val hasPhoto: Boolean,
    /** Va en el ítem solo para que cambiar de paleta lo redibuje (ver hasPhoto arriba). */
    val isDarkMap: Boolean = true
) : ClusterItem {

    private val latLng = LatLng(marker.lat, marker.lng)

    override fun getPosition(): LatLng = latLng

    /** Título y snippet son lo que lee TalkBack sobre el marcador. */
    override fun getTitle(): String = marker.vehicleName

    override fun getSnippet(): String =
        "${marker.calificacion} de 5 estrellas · ${marker.nombreUsuario} · ${marker.tiempoTexto}"

    /** El seleccionado queda por encima de sus vecinos. */
    override fun getZIndex(): Float = if (isSelected) 1f else 0f
}
