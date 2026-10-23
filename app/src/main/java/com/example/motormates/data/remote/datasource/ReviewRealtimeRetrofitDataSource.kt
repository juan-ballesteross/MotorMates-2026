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
}
