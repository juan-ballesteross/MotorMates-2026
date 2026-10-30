package com.example.motormates.data.model

data class Story(
    val id: String,
    val userId: String,
    val userName: String,
    val userAvatarUrl: String?,
    val imageUrl: String,
    val createdAt: String?,
    val likesCount: Long = 0L
)
