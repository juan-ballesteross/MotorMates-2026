package com.example.motormates.data.remote.datasource

import com.example.motormates.data.dto.ReviewDtoGeneric
import com.example.motormates.data.model.NewReview

interface ReviewRemoteDataSource {

    suspend fun getReviewsByVehicle(vehicleId: String): List<ReviewDtoGeneric>

    suspend fun getReviewsByUser(userId: String): List<ReviewDtoGeneric>

    suspend fun createReview(review: NewReview): ReviewDtoGeneric

    /** rating o comment en null significan "no cambiar este campo". */
    suspend fun updateReview(id: String, rating: Int?, comment: String?): ReviewDtoGeneric

    suspend fun deleteReview(id: String)
}
