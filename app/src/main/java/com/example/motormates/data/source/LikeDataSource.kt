package com.example.motormates.data.source

interface LikeDataSource {
    suspend fun likeReview(reviewId: String, userId: String)
}