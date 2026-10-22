package com.example.motormates.data.source

interface LikeDataSource {
    suspend fun likeReview(reviewId: String, userId: String)
    suspend fun unlikeReview(reviewId: String, userId: String)
    suspend fun getLikedReviewIds(userId: String, reviewIds: List<String>): Set<String>
}