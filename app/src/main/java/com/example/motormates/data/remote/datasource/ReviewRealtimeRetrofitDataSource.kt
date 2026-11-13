package com.example.motormates.data.remote.datasource

import com.example.motormates.data.dto.ReviewDtoGeneric
import com.example.motormates.data.remote.pollingFlow
import com.example.motormates.data.remote.service.ReviewApiService
import com.example.motormates.data.source.ReviewRealtimeDataSource
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * "Tiempo real" de reseñas contra el backend propio, con polling: las
 * mismas consultas de ReviewRetrofitDataSource repetidas cada pocos
 * segundos. El backend ya las ordena de la más reciente a la más vieja.
 */
class ReviewRealtimeRetrofitDataSource @Inject constructor(
    private val service: ReviewApiService
) : ReviewRealtimeDataSource {

    override fun observeReviewsByVehicle(vehicleId: String): Flow<List<ReviewDtoGeneric>> =
        pollingFlow { service.getReviewsByVehicle(vehicleId) }

    override fun observeReviewsByUsers(userIds: List<String>): Flow<List<ReviewDtoGeneric>> {
        if (userIds.isEmpty()) return flowOf(emptyList())
        val ids = userIds.joinToString(",")
        return pollingFlow { service.getReviewsByUsers(ids) }
    }

    /**
     * Pendiente del lado del servidor: el backend propio todavía no guarda la
     * ubicación de las reseñas ni acepta el parámetro `since`, así que esta
     * llamada falla y el repositorio la convierte en Result.failure (el mapa
     * mostraría su mensaje de error). Con ACTIVE_BACKEND = FIREBASE, que es
     * el valor actual, este código no se ejecuta.
     *
     * Se implementa igual y no con un emptyList() silencioso: así, cuando el
     * backend añada la columna y el parámetro, el mapa funciona sin tocar
     * nada acá.
     */
    override fun observeRecentReviews(sinceMillis: Long): Flow<List<ReviewDtoGeneric>> =
        pollingFlow { service.getRecentReviews(sinceMillis) }
}
