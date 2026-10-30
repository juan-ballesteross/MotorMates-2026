package com.example.motormates.data.model

data class Notification(
    val id: String,
    val type: NotificationType,
    val actorId: String,
    val actorName: String,
    val actorAvatarUrl: String?,
    val vehicleId: String?,
    val vehicleBrand: String?,
    val vehicleModel: String?,
    val vehicleImageUrl: String?,
    val reviewId: String?,
    val createdAt: String?
)
