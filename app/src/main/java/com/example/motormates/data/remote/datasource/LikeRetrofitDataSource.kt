package com.example.motormates.data.remote.datasource

import com.example.motormates.data.remote.dto.LikeRequest
import com.example.motormates.data.remote.pollingFlow
import com.example.motormates.data.remote.service.ReviewApiService
import com.example.motormates.data.remote.service.UserApiService
import com.example.motormates.data.source.LikeDataSource
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Likes contra el backend propio. Un segundo like del mismo usuario no
 * suma nada: lo rechaza la llave compuesta (reviewId, userId) de la tabla.
 */
class LikeRetrofitDataSource @Inject constructor(
    private val reviewService: ReviewApiService,
    private val userService: UserApiService
) : LikeDataSource {

    override suspend fun likeReview(reviewId: String, userId: String) {
        reviewService.likeReview(reviewId, LikeRequest(userId = userId))
    }

    override suspend fun unlikeReview(reviewId: String, userId: String) {
        reviewService.unlikeReview(reviewId, userId)
    }

    override fun observeLikedReviewIds(userId: String, reviewIds: List<String>): Flow<Set<String>> {
        if (reviewIds.isEmpty()) return flowOf(emptySet())
        val ids = reviewIds.joinToString(",")
        return pollingFlow { userService.getLikedReviewIds(userId, ids).toSet() }
    }
}
