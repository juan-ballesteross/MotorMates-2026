package com.example.motormates.data.model

import android.net.Uri

/**
 * Un evento de la comunidad: un Car Meet o un pique.
 *
 * Es UN modelo y no dos. Los dos tipos comparten título, descripción, lugar,
 * fechas, organizador y participantes, y solo se diferencian en el tipo y en
 * los atributos marcables (ver [EventFeature]). Partirlo en dos jerarquías
 * duplicaría el DTO, el formulario, las reglas de Firestore y cada mapper a
 * cambio de nada.
 *
 * Las fechas van como epoch en millis, y no como el String ISO de [Review]:
 * ahí el ISO viene impuesto por el backend propio, mientras que aquí la única
 * fuente es Firestore, que ya guarda millis, y el formulario necesita
 * comparar inicio con fin sin parsear texto.
 */
data class Event(
    val id: String,
    val type: EventType,
    val title: String,
    val description: String?,
    val imageUrl: String? = null,
    /** uid del organizador. Siempre presente: es con lo que se decide isMine. */
    val organizerId: String,
    /** Captura desnormalizada del perfil, nulable igual que [Review.author]. */
    val organizer: EventOrganizer?,
    /**
     * No nulable, al contrario que en [Review]: una reseña sin ubicación se
     * publica igual y simplemente no sale en el mapa, pero un evento sin
     * punto en el mapa no tendría dónde celebrarse.
     *
     * Se reutiliza [ReviewLocation] tal cual. Se llama así por dónde nació,
     * pero ya es el tipo de ubicación genérico de la app: lo devuelve
     * DeviceLocationDataSource y lo consume el geocodificador.
     */
    val location: ReviewLocation,
    /** El sitio que escribe el organizador ("Parque de la 93"). */
    val venueName: String?,
    val address: String?,
    val startAt: Long,
    /** Fin opcional: muchos encuentros no anuncian a qué hora terminan. */
    val endAt: Long? = null,
    val status: EventStatus = EventStatus.SCHEDULED,
    val features: Set<EventFeature> = emptySet(),
    val interestedCount: Int = 0,
    val goingCount: Int = 0,
    val createdAt: Long = 0L
)

data class EventOrganizer(
    val id: String,
    val fullName: String,
    val avatarUrl: String? = null
)

enum class EventType { CAR_MEET, PIQUE }

/** Qué tengo que ver con un evento, para la pestaña "Eventos" del perfil. */
enum class MyEventRole { ORGANIZER, GOING, INTERESTED }

data class MyEvent(val event: Event, val role: MyEventRole)

/**
 * Solo dos valores, y eso es deliberado: no hay FINISHED.
 *
 * "Cancelado" es una decisión del organizador y no se puede deducir del
 * calendario, así que se almacena. "Ya terminó", en cambio, sí se deduce de
 * las fechas, y almacenarlo obligaría a una Cloud Function programada
 * volteando miles de documentos cada hora. Se deriva al mapear a la UI.
 */
enum class EventStatus { SCHEDULED, CANCELLED }

/**
 * Lo que se puede marcar en un evento.
 *
 * Un solo enum para los dos tipos, y cada constante declara a cuál
 * pertenece: así el formulario pinta solo las que aplican
 * (`entries.filter { it.type == tipoElegido }`) y un único composable de
 * casillas sirve para Car Meets y para piques.
 *
 * Por qué no dos sub-objetos nulables (carMeet:{…} / pique:{…}): toObject()
 * tendría que mapear dos mapas anidados de puros booleanos, y cada atributo
 * nuevo sería un campo nuevo en el DTO y en las reglas. Así, agregar un
 * atributo es agregar una constante.
 *
 * Por qué una lista de claves y no un mapa { PARRILLA: true }: con el mapa no
 * se puede preguntar array-contains, y un false ocupa lugar igual.
 *
 * Ojo: a diferencia de [ReviewAspect], esto SÍ se persiste, así que renombrar
 * una constante invalida los eventos ya publicados.
 */
enum class EventFeature(val type: EventType) {
    // Car Meets: qué va a haber en el encuentro.
    EXHIBICION(EventType.CAR_MEET),
    ZONA_FOTOS(EventType.CAR_MEET),
    FOOD_TRUCKS(EventType.CAR_MEET),
    MUSICA(EventType.CAR_MEET),
    PREMIACION(EventType.CAR_MEET),
    APTO_FAMILIAS(EventType.CAR_MEET),
    PARQUEADERO(EventType.CAR_MEET),
    ENTRADA_LIBRE(EventType.CAR_MEET),

    // Piques: requisitos de competición y de seguridad.
    CRONOMETRAJE(EventType.PIQUE),
    PISTA_CERRADA(EventType.PIQUE),
    CASCO_OBLIGATORIO(EventType.PIQUE),
    ARNES_OBLIGATORIO(EventType.PIQUE),
    EXTINTOR_ABORDO(EventType.PIQUE),
    JAULA_ANTIVUELCO(EventType.PIQUE),
    AMBULANCIA_EN_SITIO(EventType.PIQUE),
    INSCRIPCION_PREVIA(EventType.PIQUE)
}

/** En qué estado está la participación de un usuario en un evento. */
enum class ParticipationStatus { INTERESTED, GOING }

/**
 * Qué hizo de verdad la escritura de participación, para que quien llama
 * pueda decidir si hay algo que avisar.
 *
 * No es un [ParticipationStatus] nulable con el estado anterior, que sería lo
 * obvio: ese tipo no distingue "antes no había nada" de "no escribí nada".
 * Volver a marcar lo que ya estaba marcado sale de la transacción sin escribir,
 * y leer eso como "antes no había nada" haría que un doble toque le avisara al
 * organizador de una participación que nunca cambió.
 */
enum class ParticipationChange {
    /** Volver a marcar lo mismo: no se escribió nada. */
    UNCHANGED,

    /** Marca nueva, de no participar a "me interesa" o "voy". Es la que avisa. */
    CREATED,

    /** Cambio entre los dos estados, que mueve los dos contadores a la vez. */
    UPDATED,

    /** Se quitó la participación. */
    REMOVED
}

/**
 * Lo que el repositorio le entrega a la fuente de datos para crear o editar
 * un evento. Igual que [NewReview], lleva la captura desnormalizada del
 * organizador para no tener que hacer JOINs al leer.
 */
data class NewEvent(
    val type: EventType,
    val title: String,
    val description: String?,
    val organizerId: String,
    val organizerName: String,
    val organizerAvatarUrl: String?,
    val location: ReviewLocation,
    val venueName: String?,
    val address: String?,
    val startAt: Long,
    val endAt: Long? = null,
    val features: Set<EventFeature> = emptySet(),
    /** Null si no se eligió imagen: es opcional. */
    val imageUri: Uri? = null,
    /** La que ya estaba publicada, al editar sin cambiar de imagen. */
    val existingImageUrl: String? = null
)
