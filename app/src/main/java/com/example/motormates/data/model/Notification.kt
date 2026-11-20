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
    val eventId: String?,
    /**
     * Título del evento, ya resuelto al mapear igual que vehicleBrand: no se
     * guarda en el documento de la notificación para que no se quede
     * congelado si el organizador renombra el evento.
     */
    val eventTitle: String?,
    val createdAt: String?
)
