package com.example.motormates.data.model

/**
 * Notificación ya resuelta: el actor y, si aplica, el vehículo ya vienen
 * con su nombre actual (se buscaron al leer, no se guardaron desnormalizados
 * como en las reseñas).
 */
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
