package com.example.motormates.data.source

import com.example.motormates.data.dto.EventParticipantDto
import com.example.motormates.data.dto.toParticipationStatusOrNull
import com.example.motormates.data.model.ParticipationChange
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

    /**
     * status null quita la participación.
     *
     * Devuelve qué pasó y no Unit porque solo la transacción sabe cuál era el
     * estado anterior, y de eso depende si hay que avisarle al organizador
     * (ver notifyOrganizer en EventRepositoryImpl). Resolverlo con una lectura
     * previa fuera de la transacción tendría carrera: dos toques rápidos
     * podrían leer los dos que no había nada y avisar dos veces.
     */
    suspend fun setParticipation(
        eventId: String,
        userId: String,
        status: ParticipationStatus?
    ): ParticipationChange

    fun observeMyParticipation(eventId: String, userId: String): Flow<ParticipationStatus?>

    /** Una sola lectura (la pestaña "Eventos" del perfil revisa varios a la vez). */
    suspend fun getParticipation(eventId: String, userId: String): ParticipationStatus?
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
    ): ParticipationChange {
        val eventRef = firestore.collection(EVENTS_COLLECTION).document(eventId)
        val participantRef = eventRef.collection(PARTICIPANTS_COLLECTION).document(userId)

        return firestore.runTransaction { transaction ->
            val snapshot = transaction.get(participantRef)
            val before = snapshot
                .takeIf { it.exists() }
                ?.getString("status")
                ?.toParticipationStatusOrNull()

            if (before == status) return@runTransaction ParticipationChange.UNCHANGED

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

            // Lo que acabó de pasar, para que el repositorio sepa si hay algo
            // que avisar. Se calcula aquí dentro porque `before` solo existe
            // dentro de la transacción.
            when {
                status == null -> ParticipationChange.REMOVED
                before == null -> ParticipationChange.CREATED
                else -> ParticipationChange.UPDATED
            }
        }.await()
    }

    override suspend fun getParticipation(eventId: String, userId: String): ParticipationStatus? =
        firestore.collection(EVENTS_COLLECTION)
            .document(eventId)
            .collection(PARTICIPANTS_COLLECTION)
            .document(userId)
            .get()
            .await()
            .getString("status")
            ?.toParticipationStatusOrNull()

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
