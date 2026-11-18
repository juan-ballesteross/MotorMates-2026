package com.example.motormates.data.source

import com.example.motormates.data.dto.EventParticipantDto
import com.example.motormates.data.dto.toParticipationStatusOrNull
import com.example.motormates.data.model.ParticipationStatus
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Quién va a cada evento.
 *
 * UNA subcolección con un campo `status`, y no dos subcolecciones
 * ("interested" y "going"), porque los dos estados son mutuamente
 * excluyentes: con una sola, pasar de "me interesa" a "voy" es un set más dos
 * increments en UNA transacción, y "¿en qué estoy?" es la lectura de un solo
 * documento. Con dos subcolecciones habría que garantizar la exclusión entre
 * ellas, lo que obliga a leer las dos en cada cambio y a expresar en las
 * reglas un "no crear en B si existe A", que cuesta una lectura facturada por
 * evaluación.
 */
interface EventParticipationDataSource {

    /** status null quita la participación. */
    suspend fun setParticipation(eventId: String, userId: String, status: ParticipationStatus?)

    fun observeMyParticipation(eventId: String, userId: String): Flow<ParticipationStatus?>
}

class FirestoreEventParticipationDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) : EventParticipationDataSource {

    /**
     * Una transacción: o se mueven el documento de participación y los
     * contadores, o no se mueve nada.
     *
     * El guardia NO es "existe el documento", como en los likes, sino "cuál
     * era el estado anterior": pasar de "me interesa" a "voy" sí tiene que
     * escribir (y mover dos contadores), mientras que volver a marcar lo
     * mismo no debe escribir nada, para que un doble toque no descuadre el
     * conteo.
     *
     * Cada contador se mueve como máximo en ±1 por escritura, que es
     * exactamente lo que exige stepsByOne() en firestore.rules.
     */
    override suspend fun setParticipation(
        eventId: String,
        userId: String,
        status: ParticipationStatus?
    ) {
        val eventRef = firestore.collection(EVENTS_COLLECTION).document(eventId)
        val participantRef = eventRef.collection(PARTICIPANTS_COLLECTION).document(userId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(participantRef)
            val before = snapshot
                .takeIf { it.exists() }
                ?.getString("status")
                ?.toParticipationStatusOrNull()

            if (before == status) return@runTransaction null

            before?.let { transaction.update(eventRef, it.counterField, FieldValue.increment(-1)) }
            status?.let { transaction.update(eventRef, it.counterField, FieldValue.increment(1)) }

            if (status == null) {
                transaction.delete(participantRef)
            } else {
                transaction.set(
                    participantRef,
                    EventParticipantDto(
                        status = status.name,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            // Una transacción debe terminar devolviendo un valor; no necesitamos ninguno.
            null
        }.await()
    }

    /** Mismo molde que observeLikeExists en FirestoreLikeDataSource. */
    override fun observeMyParticipation(
        eventId: String,
        userId: String
    ): Flow<ParticipationStatus?> = callbackFlow {
        val registration = firestore.collection(EVENTS_COLLECTION)
            .document(eventId)
            .collection(PARTICIPANTS_COLLECTION)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.getString("status")?.toParticipationStatusOrNull())
            }
        awaitClose { registration.remove() }
    }
}

/** El contador del evento que le corresponde a cada estado. */
internal val ParticipationStatus.counterField: String
    get() = when (this) {
        ParticipationStatus.INTERESTED -> "interestedCount"
        ParticipationStatus.GOING -> "goingCount"
    }
