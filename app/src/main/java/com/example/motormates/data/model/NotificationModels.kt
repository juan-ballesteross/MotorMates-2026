package com.example.motormates.data.model

import com.example.motormates.data.mock.VehicleImages

/** Tipo de evento que generó la notificación — determina el ícono mostrado. */
enum class NotificationType { LIKE, COMMENT, FOLLOW, RATING }

data class NotificationUi(
    val id: String,
    val type: NotificationType,
    val actorName: String,
    val message: String,
    val timeAgo: String,
    val section: String, // ej. "HOY", "ESTA SEMANA" — agrupa la lista
    val thumbnailResId: Int? = null // null = sin miniatura; 0 = con miniatura pero foto pendiente; id real = foto ya puesta
)

// ===== Datos locales quemados =====

val mockNotifications = listOf(
    NotificationUi(
        id = "1",
        type = NotificationType.LIKE,
        actorName = "Elena Cruz",
        message = "le dio like a tu reseña del Porsche 911 GT3",
        timeAgo = "10 min",
        section = "HOY"
    ),
    NotificationUi(
        id = "2",
        type = NotificationType.COMMENT,
        actorName = "Diego Salas",
        message = "comentó: \"Totalmente de acuerdo con lo del PDK\"",
        timeAgo = "45 min",
        section = "HOY",
        thumbnailResId = VehicleImages.PORSCHE_911_GT3
    ),
    NotificationUi(
        id = "3",
        type = NotificationType.FOLLOW,
        actorName = "Marco Ferretti",
        message = "empezó a seguirte",
        timeAgo = "2 h",
        section = "HOY"
    ),
    NotificationUi(
        id = "4",
        type = NotificationType.RATING,
        actorName = "Camila Ortiz",
        message = "calificó tu reseña como útil",
        timeAgo = "2 días",
        section = "ESTA SEMANA"
    ),
    NotificationUi(
        id = "5",
        type = NotificationType.COMMENT,
        actorName = "Iván Torres",
        message = "comentó en tu publicación del M3 Competition",
        timeAgo = "4 días",
        section = "ESTA SEMANA",
        thumbnailResId = VehicleImages.M3_COMPETITION
    )
)