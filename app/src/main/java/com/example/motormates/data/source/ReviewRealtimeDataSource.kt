package com.example.motormates.data.source

import com.example.motormates.data.dto.ReviewDtoGeneric
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
 * porque cada fuente lo resuelve distinto: Firestore con listeners y el
 * backend propio con polling (ReviewRealtimeRetrofitDataSource). Devuelve
 * el contrato común ReviewDtoGeneric para no amarrar a nadie a Firestore.
 */
interface ReviewRealtimeDataSource {
    fun observeReviewsByVehicle(vehicleId: String): Flow<List<ReviewDtoGeneric>>
    fun observeReviewsByUsers(userIds: List<String>): Flow<List<ReviewDtoGeneric>>

    /**
     * Todas las reseñas publicadas desde sinceMillis (epoch en millis), sin
     * importar de quién o de qué vehículo. Es la consulta del mapa: no filtra
     * por ubicación porque Firestore no sabe preguntar "donde location no sea
     * null"; las que no la tengan se descartan al mapear a la UI.
     */
    fun observeRecentReviews(sinceMillis: Long): Flow<List<ReviewDtoGeneric>>
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
     * La consulta del mapa. Es la única del proyecto que usa orderBy, y sí se
     * puede: el campo del orden es el mismo del filtro de rango (createdAt),
     * así que lo resuelve el índice de un solo campo que Firestore crea solo
     * — no hace falta índice compuesto, que es justo lo que se evita en el
     * resto de las consultas.
     *
     * Y hace falta: Firestore ordena de forma implícita por el campo del
     * filtro de rango de manera ASCENDENTE, así que sin este orderBy el
     * limit() se traería las reseñas más VIEJAS de la ventana en vez de las
     * más recientes.
     *
     * Los documentos sin createdAt (o con un tipo distinto) quedan fuera del
     * filtro de rango, que es exactamente lo que se quiere.
     */
    override fun observeRecentReviews(sinceMillis: Long): Flow<List<ReviewFirestoreDto>> =
        observeQuery(
            reviews
                .whereGreaterThanOrEqualTo("createdAt", sinceMillis)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(RECENT_REVIEWS_LIMIT)
        )

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

        /**
         * Techo de seguridad del mapa: con la ventana de 24 h se esperan
         * pocas reseñas, pero sin limit un pico de actividad abriría un
         * listener sobre cientos de documentos.
         */
        const val RECENT_REVIEWS_LIMIT = 200L
    }
}
