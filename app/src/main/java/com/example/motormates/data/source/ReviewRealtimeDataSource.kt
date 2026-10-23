package com.example.motormates.data.source

import com.example.motormates.data.dto.ReviewFirestoreDto
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

/**
 * Lectura de reseñas en tiempo real. Va aparte de ReviewRemoteDataSource
 * porque esa interfaz también la implementa la versión de Retrofit, que no
 * puede escuchar cambios: un listener solo tiene sentido contra Firestore.
 */
interface ReviewRealtimeDataSource {
    fun observeReviewsByVehicle(vehicleId: String): Flow<List<ReviewFirestoreDto>>
    fun observeReviewsByUsers(userIds: List<String>): Flow<List<ReviewFirestoreDto>>
}

class FirestoreReviewRealtimeDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) : ReviewRealtimeDataSource {

    private val reviews get() = firestore.collection(REVIEWS_COLLECTION)

    override fun observeReviewsByVehicle(vehicleId: String): Flow<List<ReviewFirestoreDto>> =
        observeQuery(reviews.whereEqualTo("vehicleId", vehicleId))

    /**
     * whereIn acepta máximo 30 valores, así que la lista de seguidos se
     * parte en bloques y se escucha uno por bloque; combine vuelve a juntar
     * los resultados cada vez que cualquiera de ellos cambia. Sin seguidos
     * no se abre ningún listener.
     */
    override fun observeReviewsByUsers(userIds: List<String>): Flow<List<ReviewFirestoreDto>> {
        if (userIds.isEmpty()) return flowOf(emptyList())

        val chunks = userIds.chunked(WHERE_IN_LIMIT).map { chunk ->
            observeQuery(reviews.whereIn("userId", chunk))
        }

        return if (chunks.size == 1) {
            chunks.first()
        } else {
            combine(chunks) { results -> results.toList().flatten().sortedByDescending { it.createdAt } }
        }
    }

    /**
     * Mismo molde que authState en FirebaseAuthDataSource: el listener se
     * registra al empezar a recolectar y se quita en awaitClose, para que
     * no queden escuchas vivas cuando la pantalla se va.
     *
     * Se ordena en memoria (más recientes primero) en vez de con orderBy:
     * whereEqualTo/whereIn + orderBy sobre otro campo exige crear un índice
     * compuesto en la consola.
     */
    private fun observeQuery(query: Query): Flow<List<ReviewFirestoreDto>> = callbackFlow {
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val dtos = snapshot?.toObjects(ReviewFirestoreDto::class.java).orEmpty()
            trySend(dtos.sortedByDescending { it.createdAt })
        }
        awaitClose { registration.remove() }
    }

    private companion object {
        const val WHERE_IN_LIMIT = 30
    }
}
