package com.example.motormates.data.model

/**
 * Un evento listo para pintarse como marcador y como tarjeta en el mapa.
 *
 * Igual que [ReviewMapMarkerUi], lat y lng van como Double y no como LatLng:
 * este paquete lo consumen las pruebas de JVM puro.
 *
 * Los textos ya vienen formateados y los estados ya vienen resueltos
 * (`isMine`, `hasFinished`), porque eso se decide en el mapper y no en la
 * pantalla: es la misma convención de `tiempoTexto` y de `ReviewUi.isMine`.
 */
data class EventMapMarkerUi(
    val id: String,
    val type: EventType,
    val lat: Double,
    val lng: Double,
    val title: String,
    val descripcion: String? = null,
    val imageUrl: String? = null,
    val organizerId: String,
    val nombreOrganizador: String,
    val avatarUrl: String? = null,
    /** Dónde es, ya legible: el sitio que escribió el organizador o el barrio. */
    val lugarTexto: String? = null,
    /** "mañana, 7:00 p. m." */
    val fechaTexto: String,
    /** "de 7:00 p. m. a 11:00 p. m." Null si no se anunció hora de fin. */
    val horarioTexto: String? = null,
    val features: List<EventFeature> = emptyList(),
    val interestedCount: Int = 0,
    val goingCount: Int = 0,
    /** El organizador es quien está viendo: solo él ve editar y cancelar. */
    val isMine: Boolean = false,
    val isCancelled: Boolean = false,
    /** Derivado de las fechas, nunca almacenado (ver [EventStatus]). */
    val hasFinished: Boolean = false
)
