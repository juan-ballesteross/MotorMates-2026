package com.example.motormates.data.model

data class StoryUser(val name: String)

data class ReviewPost(
    val userName: String,
    val carName: String,
    val timeAgo: String,
    val rating: Int,
    val caption: String,
    val likes: Int,
    val comments: Int,
    val shares: Int
)
