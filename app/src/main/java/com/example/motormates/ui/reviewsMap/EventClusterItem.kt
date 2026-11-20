package com.example.motormates.ui.reviewsMap

import com.example.motormates.data.model.EventMapMarkerUi
import com.example.motormates.data.model.EventType
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

/**
 * Adapta un evento a lo que pide la librería de clustering.
 *
 * Es un data class por la misma razón que [ReviewClusterItem]: el contenido
 * del marcador se rasteriza una sola vez, así que lo que fuerza a redibujarlo
 * es que el ítem deje de ser igual al anterior. Si fuera una clase normal,
 * cambiar la selección no repintaría nada.
 */
data class EventClusterItem(
    val event: EventMapMarkerUi,
    val isSelected: Boolean,
    val isDarkMap: Boolean = true
) : ClusterItem {

    private val latLng = LatLng(event.lat, event.lng)

    override fun getPosition(): LatLng = latLng

    override fun getTitle(): String = event.title

    override fun getSnippet(): String = listOfNotNull(
        when (event.type) {
            EventType.CAR_MEET -> "Car Meet"
            EventType.PIQUE -> "Pique"
        },
        event.fechaTexto,
        event.lugarTexto
    ).joinToString(" · ")

    /**
     * Por encima de los marcadores de reseña (que usan 0 y 1): si un evento y
     * una reseña caen en el mismo punto, el evento no queda escondido debajo.
     */
    override fun getZIndex(): Float = if (isSelected) 3f else 2f
}
