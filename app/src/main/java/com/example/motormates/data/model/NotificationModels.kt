package com.example.motormates.data.model

import com.example.motormates.data.mock.VehicleImages

/**
 * Tipo de evento que generó la notificación — determina el ícono mostrado.
 *
 * La participación en eventos son DOS valores (EVENT_INTERESTED y EVENT_GOING)
 * y no uno solo con un campo de estado al lado. El motivo es el compilador:
 * hay dos `when` exhaustivos sin `else` sobre este enum (el del mensaje en
 * UiMappers.kt y el del ícono en AlertsComponents.kt), así que agregar una
 * constante obliga a pasar por los dos. Con un campo aparte los dos `when`
 * seguirían compilando y el mensaje saldría incompleto en silencio.
 *
 * Agregar constantes es seguro para las versiones viejas de la app: un tipo
 * que no se reconoce descarta esa fila al mapear (ver el mapNotNull de
 * NotificationRepositoryImpl.toDomain), no tumba la lista.
 */
enum class NotificationType {
    LIKE, COMMENT, FOLLOW, RATING, REVIEW, LIKE_STORY, MESSAGE, EVENT_INTERESTED, EVENT_GOING
}

data class NotificationUi(
    val id: String,
    val type: NotificationType,
    val actorId: String = "",
    val actorName: String,
    val actorAvatarUrl: String? = null,
    val message: String,
    val timeAgo: String,
    val section: String, // ej. "HOY", "ESTA SEMANA" — agrupa la lista
    val thumbnailResId: Int? = null, // null = sin miniatura; 0 = con miniatura pero foto pendiente; id real = foto ya puesta
    val thumbnailUrl: String? = null,
    /** Con qué evento navegar al detalle. Null en los tipos que no son de evento. */
    val eventId: String? = null
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