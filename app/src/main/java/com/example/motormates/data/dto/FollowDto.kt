package com.example.motormates.data.dto

import com.google.firebase.firestore.DocumentId

data class FollowDto(
    @DocumentId val id: String = "",
    val createdAt: Long = 0L
)
