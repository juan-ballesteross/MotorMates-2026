package com.example.motormates.data.remote.datasource

import com.example.motormates.data.remote.dto.CreateReviewRequest
import com.example.motormates.data.remote.dto.ReviewDto
import com.example.motormates.data.remote.dto.UpdateReviewRequest

interface ReviewRemoteDataSource {

    suspend fun getReviewsByVehicle(vehicleId: Int): List<ReviewDto>

    suspend fun getReviewsByUser(userId: Int): List<ReviewDto>

    suspend fun createReview(body: CreateReviewRequest): ReviewDto

    suspend fun updateReview(id: Int, body: UpdateReviewRequest): ReviewDto

    suspend fun deleteReview(id: Int)
}
