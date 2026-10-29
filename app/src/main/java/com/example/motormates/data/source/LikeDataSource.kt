package com.example.motormates.data.source

import kotlinx.coroutines.flow.Flow

interface LikeDataSource {
    suspend fun likeReview(reviewId: String, userId: String)
    suspend fun unlikeReview(reviewId: String, userId: String)

    /**
     * De estas reseñas, a cuáles les ha dado like el usuario, escuchando:
     * vuelve a emitir cuando da o quita un like, aunque sea desde otro
     * dispositivo.
     */
    fun observeLikedReviewIds(userId: String, reviewIds: List<String>): Flow<Set<String>>
}
