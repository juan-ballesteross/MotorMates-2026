package com.example.motormates.data.remote.datasource

import com.example.motormates.data.dto.ReviewDtoGeneric
import com.example.motormates.data.model.NewReview
import com.example.motormates.data.remote.dto.CreateReviewRequest
import com.example.motormates.data.remote.dto.UpdateReviewRequest
import com.example.motormates.data.remote.service.ReviewApiService
import javax.inject.Inject

/**
 * Implementación contra el backend propio (Postgres). Los ids viajan como
 * String igual que en el dominio: el userId es el uid de Firebase y el
 * backend convierte los ids numéricos de reviews y vehículos por su cuenta.
 */
class ReviewRetrofitDataSource @Inject constructor(
    private val service: ReviewApiService
) : ReviewRemoteDataSource {

    override suspend fun getReviewsByVehicle(vehicleId: String): List<ReviewDtoGeneric> =
        service.getReviewsByVehicle(vehicleId)

    override suspend fun getReviewsByUser(userId: String): List<ReviewDtoGeneric> =
        service.getReviewsByUser(userId)

    /**
     * El backend no guarda la captura del autor ni del vehículo (los trae
     * con un JOIN al leer), así que authorName/vehicleBrand no se envían.
     */
    override suspend fun createReview(review: NewReview): ReviewDtoGeneric =
        service.createReview(
            CreateReviewRequest(
                userId = review.userId,
                vehicleId = review.vehicleId,
                rating = review.rating,
                comment = review.comment
            )
        )

    override suspend fun updateReview(id: String, rating: Int?, comment: String?): ReviewDtoGeneric =
        service.updateReview(id, UpdateReviewRequest(rating = rating, comment = comment))

    override suspend fun deleteReview(id: String) = service.deleteReview(id)
}
