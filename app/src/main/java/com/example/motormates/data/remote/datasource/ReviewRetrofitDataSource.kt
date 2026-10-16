package com.example.motormates.data.remote.datasource

import com.example.motormates.data.dto.ReviewDtoGeneric
import com.example.motormates.data.model.NewReview
import com.example.motormates.data.remote.dto.CreateReviewRequest
import com.example.motormates.data.remote.dto.UpdateReviewRequest
import com.example.motormates.data.remote.service.ReviewApiService
import javax.inject.Inject

/**
 * Implementación contra el backend propio (Postgres). Sus ids son
 * numéricos, así que los ids String del dominio se convierten aquí.
 */
class ReviewRetrofitDataSource @Inject constructor(
    private val service: ReviewApiService
) : ReviewRemoteDataSource {

    override suspend fun getReviewsByVehicle(vehicleId: String): List<ReviewDtoGeneric> =
        service.getReviewsByVehicle(vehicleId.toInt())

    override suspend fun getReviewsByUser(userId: String): List<ReviewDtoGeneric> =
        service.getReviewsByUser(userId.toInt())

    override suspend fun createReview(review: NewReview): ReviewDtoGeneric =
        service.createReview(
            CreateReviewRequest(
                userId = review.userId.toInt(),
                vehicleId = review.vehicleId.toInt(),
                rating = review.rating,
                comment = review.comment
            )
        )

    override suspend fun updateReview(id: String, rating: Int?, comment: String?): ReviewDtoGeneric =
        service.updateReview(id.toInt(), UpdateReviewRequest(rating = rating, comment = comment))

    override suspend fun deleteReview(id: String) = service.deleteReview(id.toInt())
}
