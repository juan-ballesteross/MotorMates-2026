package com.example.motormates.data.source

import android.net.Uri
import com.example.motormates.data.dto.EventFirestoreDto
import com.example.motormates.data.dto.ReviewLocationSnapshot
import com.example.motormates.data.dto.ReviewUserSnapshot
import com.example.motormates.data.model.NewEvent
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Los eventos publicados por la comunidad.
 *
 * No pasa por el `pick(firebase, express)` de DataSourceModule: ahí van las
 * fuentes que existen en los dos backends, y esta solo existe en Firestore.
 * Se enlaza directo, igual que PushTokenDataSource y DeviceLocationDataSource.
 */
interface EventDataSource {

    /**
     * Los eventos cuyo inicio cae en la ventana dada, del más próximo al más
     * lejano. No filtra por tipo ni por estado: eso se decide en memoria (ver
     * [FirestoreEventDataSource.observeEventsBetween]).
     */
    fun observeEventsBetween(fromMillis: Long, toMillis: Long): Flow<List<EventFirestoreDto>>

    suspend fun getEventById(eventId: String): EventFirestoreDto

    suspend fun createEvent(event: NewEvent): EventFirestoreDto

    suspend fun updateEvent(eventId: String, event: NewEvent): EventFirestoreDto

    suspend fun cancelEvent(eventId: String)
}

class FirestoreEventDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : EventDataSource {

    private val events get() = firestore.collection(EVENTS_COLLECTION)

    /**
     * Dos cotas de rango sobre el MISMO campo (`startAt`) más un orderBy por
     * ese mismo campo: eso lo resuelve el índice de un campo que Firestore
     * crea solo, sin índice compuesto. Añadir aquí un whereEqualTo de `type`
     * o de `status` sí lo volvería compuesto, así que esos dos se filtran en
     * memoria, en el ViewModel.
     *
     * ASCENDING, al contrario que las reseñas, y conviene no "arreglarlo":
     * observeRecentReviews ordena descendente porque dentro del limit quiere
     * las MÁS NUEVAS; aquí se quieren los MÁS PRÓXIMOS.
     */
    override fun observeEventsBetween(
        fromMillis: Long,
        toMillis: Long
    ): Flow<List<EventFirestoreDto>> = callbackFlow {
        val registration = events
            .whereGreaterThanOrEqualTo("startAt", fromMillis)
            .whereLessThanOrEqualTo("startAt", toMillis)
            .orderBy("startAt", Query.Direction.ASCENDING)
            .limit(EVENTS_LIMIT)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val dtos = snapshot?.toObjects(EventFirestoreDto::class.java).orEmpty()
                trySend(dtos)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun getEventById(eventId: String): EventFirestoreDto =
        events.document(eventId).get().await()
            .toObject(EventFirestoreDto::class.java)
            ?: throw NoSuchElementException("No existe el evento $eventId")

    override suspend fun createEvent(event: NewEvent): EventFirestoreDto {
        val now = System.currentTimeMillis()
        // El id se genera antes de guardar, igual que en las reseñas, para
        // poder subir la imagen a "events/{id}/..." y guardar su URL ya
        // resuelta en el mismo documento en vez de hacer un update aparte.
        val eventId = events.document().id
        val dto = event.toDto(
            eventId = eventId,
            imageUrl = uploadImage(eventId, event.imageUri) ?: event.existingImageUrl,
            createdAt = now,
            updatedAt = now
        )
        events.document(eventId).set(dto).await()
        return dto
    }

    override suspend fun updateEvent(eventId: String, event: NewEvent): EventFirestoreDto {
        val current = getEventById(eventId)
        val dto = event.toDto(
            eventId = eventId,
            // Si no se eligió imagen nueva se conserva la que ya tenía el
            // documento, no la que venga en el formulario: así editar sin
            // tocar la foto no la borra.
            imageUrl = uploadImage(eventId, event.imageUri) ?: event.existingImageUrl ?: current.imageUrl,
            createdAt = current.createdAt,
            updatedAt = System.currentTimeMillis(),
            // Editar no revive un evento cancelado ni toca los contadores.
            status = current.status,
            interestedCount = current.interestedCount,
            goingCount = current.goingCount
        )
        events.document(eventId).set(dto).await()
        return dto
    }

    /**
     * Cancelar no borra: quien marcó "voy a asistir" necesita enterarse de
     * que ya no hay evento, y un documento desaparecido no avisa de nada.
     */
    override suspend fun cancelEvent(eventId: String) {
        events.document(eventId).update(
            mapOf(
                "status" to EVENT_STATUS_CANCELLED,
                "updatedAt" to System.currentTimeMillis()
            )
        ).await()
    }

    private suspend fun uploadImage(eventId: String, imageUri: Uri?): String? {
        val uri = imageUri ?: return null
        val imageRef = storage.reference.child("events/$eventId/${UUID.randomUUID()}.jpg")
        imageRef.putFile(uri).await()
        return imageRef.downloadUrl.await().toString()
    }

    private fun NewEvent.toDto(
        eventId: String,
        imageUrl: String?,
        createdAt: Long,
        updatedAt: Long,
        status: String = EventFirestoreDto.STATUS_SCHEDULED,
        interestedCount: Int = 0,
        goingCount: Int = 0
    ) = EventFirestoreDto(
        id = eventId,
        type = type.name,
        title = title,
        description = description,
        imageUrl = imageUrl,
        userId = organizerId,
        user = ReviewUserSnapshot(fullName = organizerName, avatarUrl = organizerAvatarUrl),
        location = ReviewLocationSnapshot(
            lat = location.lat,
            lng = location.lng,
            placeName = location.placeName
        ),
        venueName = venueName,
        address = address,
        startAt = startAt,
        endAt = endAt,
        status = status,
        features = features.map { it.name }.sorted(),
        interestedCount = interestedCount,
        goingCount = goingCount,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private companion object {
        /** Tope de seguridad, igual que el de las reseñas en el mapa. */
        const val EVENTS_LIMIT = 200L
        const val EVENT_STATUS_CANCELLED = "CANCELLED"
    }
}
