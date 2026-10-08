package com.example.motormates.data.remote

import com.example.motormates.data.remote.dto.CreateReviewRequest
import com.example.motormates.data.remote.dto.ReviewDto
import com.example.motormates.data.remote.dto.UpdateReviewRequest
import javax.inject.Inject

interface ReviewRemoteDataSource {

    suspend fun getReviewsByVehicle(vehicleId: Int): List<ReviewDto>

    suspend fun getReviewsByUser(userId: Int): List<ReviewDto>

    suspend fun createReview(body: CreateReviewRequest): ReviewDto

    suspend fun updateReview(id: Int, body: UpdateReviewRequest): ReviewDto

    suspend fun deleteReview(id: Int)
}

class ReviewRetrofitDataSource @Inject constructor(
    private val service: ReviewApiService
) : ReviewRemoteDataSource {

    override suspend fun getReviewsByVehicle(vehicleId: Int): List<ReviewDto> =
        service.getReviewsByVehicle(vehicleId)

    override suspend fun getReviewsByUser(userId: Int): List<ReviewDto> =
        service.getReviewsByUser(userId)

    override suspend fun createReview(body: CreateReviewRequest): ReviewDto =
        service.createReview(body)

    override suspend fun updateReview(id: Int, body: UpdateReviewRequest): ReviewDto =
        service.updateReview(id, body)

    override suspend fun deleteReview(id: Int) = service.deleteReview(id)
}
