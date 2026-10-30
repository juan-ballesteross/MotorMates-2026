package com.example.motormates.data.dto

import com.google.firebase.firestore.DocumentId

data class NotificationDto(
    @DocumentId val id: String = "",
    val type: String = "",
    val actorId: String = "",
    val vehicleId: String? = null,
    val reviewId: String? = null,
    val createdAt: Long = 0L
)
