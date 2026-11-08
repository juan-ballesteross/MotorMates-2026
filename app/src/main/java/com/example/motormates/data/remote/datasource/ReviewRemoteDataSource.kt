package com.example.motormates.data.remote.datasource

import android.net.Uri
import com.example.motormates.data.dto.ReviewDtoGeneric
import com.example.motormates.data.model.NewReview

interface ReviewRemoteDataSource {

    suspend fun getReviewsByVehicle(vehicleId: String): List<ReviewDtoGeneric>

    suspend fun getReviewsByUser(userId: String): List<ReviewDtoGeneric>

    suspend fun createReview(review: NewReview): ReviewDtoGeneric

    /** rating o comment en null significan "no cambiar este campo". */
    suspend fun updateReview(
        id: String,
        rating: Int?,
        comment: String?,
        keepImageUrls: List<String> = emptyList(),
        newImageUris: List<Uri> = emptyList()
    ): ReviewDtoGeneric

    suspend fun deleteReview(id: String)
}
