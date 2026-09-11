package com.example.motormates.data.model

data class StoryUser(
    val name: String,
    val avatarResId: Int,
    val imageRes: Int? = null,
    val caption: String? = null
)

data class ReviewPost(
    val userName: String,
    val avatarResId: Int,
    val carName: String,
    val imageResId: Int,
    val timeAgo: String,
    val rating: Int,
    val caption: String,
    val likes: Int,
    val comments: Int,
    val shares: Int
)
