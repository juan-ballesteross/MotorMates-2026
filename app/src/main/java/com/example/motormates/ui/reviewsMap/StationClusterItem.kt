package com.example.motormates.ui.reviewsMap

import com.example.motormates.data.model.StationMapMarkerUi
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

/**
 * Adapta una estación a lo que pide la librería de clustering.
 *
 * Es un data class por la misma razón que [ReviewClusterItem] y
 * [EventClusterItem]: el contenido del marcador se rasteriza una sola vez, así
 * que lo que fuerza a redibujarlo es que el ítem deje de ser igual al anterior.
 * Si fuera una clase normal, cambiar la selección no repintaría nada.
 */
data class StationClusterItem(
    val station: StationMapMarkerUi,
    val isSelected: Boolean,
    val isDarkMap: Boolean = true
) : ClusterItem {

    private val latLng = LatLng(station.lat, station.lng)

    override fun getPosition(): LatLng = latLng

    override fun getTitle(): String = station.nombre

    override fun getSnippet(): String = station.direccion.orEmpty()

    /**
     * Por DEBAJO de las otras dos capas (las reseñas usan 0 y 1, los eventos 2
     * y 3): si una gasolinera cae encima de una reseña o de un evento, el que
     * tiene que quedar visible es el otro. Las estaciones son el telón de
     * fondo, y además son las más numerosas.
     */
    override fun getZIndex(): Float = if (isSelected) 0.5f else -1f
}
