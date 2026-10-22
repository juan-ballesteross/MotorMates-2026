package com.example.motormates.data.dto

import com.google.firebase.firestore.DocumentId


data class UserDto(
    @DocumentId val id: String = "",
    val fullName: String = "",
    val email: String = "",
    val username: String = "",
    val location: String = "",
    val bio: String = "",
    val avatarUrl: String? = null, // URL (String), no un recurso local
    val coverUrl: String? = null,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val createdAt: Long = 0L
)