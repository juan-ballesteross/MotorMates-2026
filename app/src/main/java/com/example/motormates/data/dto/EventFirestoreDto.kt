package com.example.motormates.data.dto

import com.example.motormates.data.model.Event
import com.example.motormates.data.model.EventFeature
import com.example.motormates.data.model.EventOrganizer
import com.example.motormates.data.model.EventStatus
import com.example.motormates.data.model.EventType
import com.example.motormates.data.model.ParticipationStatus
import com.google.firebase.firestore.DocumentId

/**
 * Documento de la colección "events".
 *
 * El organizador se guarda en `userId` + `user`, con los mismos nombres que
 * en "reviews" y reutilizando [ReviewUserSnapshot]: así las reglas de
 * Firestore se leen igual en las dos colecciones, y la Cloud Function que
 * repara el perfil desnormalizado puede recorrer ambas con las mismas rutas
 * de campo.
 *
 * La ubicación reutiliza [ReviewLocationSnapshot], que ya hace lo correcto:
 * lat y lng nulables, para que un documento mal escrito se descarte en vez de
 * caer en el golfo de Guinea.
 */
data class EventFirestoreDto(
    @DocumentId val id: String = "",
    /**
     * Texto y no el enum. Firestore sabe deserializar enums, pero LANZA si el
     * valor no corresponde a ninguna constante, y una sola excepción al mapear
     * mata el snapshot entero y con él el listener. Guardando texto, un
     * documento escrito por una versión más nueva de la app (type: "RODADA")
     * se descarta solo.
     */
    val type: String = "",
    val title: String = "",
    val description: String? = null,
    val imageUrl: String? = null,
    val userId: String = "",
    val user: ReviewUserSnapshot? = null,
    val location: ReviewLocationSnapshot? = null,
    val venueName: String? = null,
    val address: String? = null,
    val startAt: Long = 0L,
    val endAt: Long? = null,
    val status: String = STATUS_SCHEDULED,
    /** Claves de [EventFeature]. Las desconocidas se descartan al mapear. */
    val features: List<String> = emptyList(),
    val interestedCount: Int = 0,
    val goingCount: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {

    /**
     * Null si el documento está mal formado: sin ubicación, sin título, sin
     * fecha de inicio o con un tipo que esta versión de la app no conoce.
     *
     * Es el segundo mapper del proyecto que devuelve null, junto con
     * `toReviewMapMarkerUi`. El criterio es el mismo: descartar en silencio
     * es preferible a tumbar la pantalla por un documento suelto.
     */
    fun toDomain(): Event? {
        val parsedType = type.toEventTypeOrNull() ?: return null
        val parsedLocation = location?.toDomain() ?: return null
        if (title.isBlank() || startAt <= 0L) return null

        return Event(
            id = id,
            type = parsedType,
            title = title,
            description = description?.ifBlank { null },
            imageUrl = imageUrl?.ifBlank { null },
            organizerId = userId,
            organizer = user?.let {
                EventOrganizer(id = userId, fullName = it.fullName, avatarUrl = it.avatarUrl)
            },
            location = parsedLocation,
            venueName = venueName?.ifBlank { null },
            address = address?.ifBlank { null },
            startAt = startAt,
            // Un fin anterior al inicio es un dato corrupto: se trata como si
            // no se hubiera anunciado hora de fin, que es lo que ya significa
            // null, en vez de propagar un rango imposible a la UI.
            endAt = endAt?.takeIf { it > startAt },
            status = status.toEventStatusOrDefault(),
            features = features.mapNotNullTo(mutableSetOf()) { it.toEventFeatureOrNull() },
            interestedCount = interestedCount,
            goingCount = goingCount,
            createdAt = createdAt
        )
    }

    companion object {
        const val STATUS_SCHEDULED = "SCHEDULED"
    }
}

/** Documento de "events/{id}/participants/{uid}". */
data class EventParticipantDto(
    @DocumentId val id: String = "",
    val status: String = "",
    val createdAt: Long = 0L
)

private fun String.toEventTypeOrNull(): EventType? =
    EventType.entries.firstOrNull { it.name == this }

/**
 * Un estado desconocido se lee como programado y no se descarta: perder el
 * evento entero por no entender su estado sería peor que mostrarlo.
 */
private fun String.toEventStatusOrDefault(): EventStatus =
    EventStatus.entries.firstOrNull { it.name == this } ?: EventStatus.SCHEDULED

private fun String.toEventFeatureOrNull(): EventFeature? =
    EventFeature.entries.firstOrNull { it.name == this }

internal fun String.toParticipationStatusOrNull(): ParticipationStatus? =
    ParticipationStatus.entries.firstOrNull { it.name == this }
