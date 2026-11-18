package com.example.motormates.data.model

/**
 * Una estación lista para pintarse como marcador y como tarjeta en el mapa.
 *
 * Igual que [EventMapMarkerUi], lat y lng van como Double y no como LatLng:
 * este paquete lo consumen las pruebas de JVM puro.
 *
 * Los textos ya vienen resueltos, con su respaldo puesto, porque eso se
 * decide en el mapper y no en la pantalla: es la misma convención de
 * `fechaTexto` y de `lugarTexto`.
 */
data class StationMapMarkerUi(
    /** El place_id de Google: identifica el marcador y abre la ficha en otra app. */
    val id: String,
    val kind: StationKind,
    val lat: Double,
    val lng: Double,
    /** Nunca vacío: si Google no manda nombre, queda el genérico. */
    val nombre: String,
    /** Null si Google no mandó dirección; la tarjeta omite la línea. */
    val direccion: String? = null
)
