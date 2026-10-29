package com.example.motormates.data.model

import com.example.motormates.data.util.toRelativeTimeEs
import java.time.Duration
import java.time.Instant

/**
 * Mapeo dominio -> UI. Está separado de DtoMappers para que quede claro
 * que son dos saltos distintos: DTO -> dominio ocurre en la capa de datos,
 * dominio -> UI ocurre al borde de la capa de presentación.
 */

fun Vehicle.toFeedVehicleUi(): FeedVehicleUi = FeedVehicleUi(
    id = id,
    brand = brand,
    model = model,
    year = year,
    category = category,
    imageUrl = imageUrl
)

/**
 * La calificación y el número de reseñas no vienen del endpoint de
 * vehículos: se calculan con las reseñas que ya se consultaron.
 * Las especificaciones técnicas sí las manda el backend, ya formateadas con
 * su unidad, así que solo se les pone "—" cuando la columna viene vacía.
 */
fun Vehicle.toCarDetailUi(reviews: List<Review>): CarDetailUi = CarDetailUi(
    marca = brand.uppercase(),
    modelo = model,
    anio = year,
    categoria = category,
    imageUrl = imageUrl,
    calificacion = if (reviews.isEmpty()) 0f else reviews.map { it.rating }.average().toFloat(),
    numeroResenas = reviews.size,
    potencia = potencia ?: "—",
    aceleracion = aceleracion ?: "—",
    velocidadMaxima = velocidadMaxima ?: "—",
    traccion = traccion ?: "—"
)

fun Vehicle.toReviewCarSummary(): ReviewCarSummary = ReviewCarSummary(
    title = "$brand $model",
    year = year,
    categoryLabel = category,
    imageUrl = imageUrl
)

/**
 * currentUserId es el uid de Firebase Auth de quien usa la app (null si no
 * hay sesión): con él se decide si la reseña es mía y se puede editar.
 */
fun Review.toReviewUi(currentUserId: String?, likedReviewIds: Set<String> = emptySet()): ReviewUi = ReviewUi(
    id = id,
    userId = userId,
    nombreUsuario = author?.fullName?.ifBlank { null } ?: "Usuario",
    avatarUrl = author?.avatarUrl,
    tiempoTexto = createdAt.toRelativeTimeEs(),
    calificacion = rating.coerceIn(0, 5),
    comentario = comment.orEmpty(),
    isMine = userId == currentUserId,
    likesCount = likesCount,
    isLikedByMe = id in likedReviewIds
)

/**
 * La reseña como se ve en el feed de "Siguiendo": lleva autor y vehículo.
 * vehicleImageUrl no viaja dentro de la reseña, la resuelve el ViewModel con
 * los vehículos, igual que en toUserReviewUi.
 */
fun Review.toFeedReviewUi(
    vehicleImageUrl: String? = null,
    likedReviewIds: Set<String> = emptySet()
): FeedReviewUi = FeedReviewUi(
    id = id,
    userId = userId,
    vehicleId = vehicleId,
    nombreUsuario = author?.fullName?.ifBlank { null } ?: "Usuario",
    avatarUrl = author?.avatarUrl,
    vehicleName = vehicle?.let { "${it.brand} ${it.model}" } ?: "Vehículo",
    vehicleImageUrl = vehicleImageUrl,
    tiempoTexto = createdAt.toRelativeTimeEs(),
    calificacion = rating.coerceIn(0, 5),
    comentario = comment.orEmpty(),
    likesCount = likesCount,
    isLikedByMe = id in likedReviewIds
)

/**
 * vehicleImageUrl no viaja dentro de la reseña (las ya guardadas en
 * Firestore no la tienen): el ViewModel la resuelve con los vehículos.
 */
fun Review.toUserReviewUi(vehicleImageUrl: String? = null): UserReviewUi = UserReviewUi(
    id = id,
    vehicleId = vehicleId,
    vehicleName = vehicle?.let { "${it.brand} ${it.model}" } ?: "Vehículo",
    vehicleImageUrl = vehicleImageUrl,
    rating = rating.coerceIn(0, 5),
    comment = comment.orEmpty(),
    timeAgo = createdAt.toRelativeTimeEs()
)

/** Perfil de un usuario, venga de Firestore o del backend propio. */
fun User.toUserProfile(reviewsCount: Int): UserProfile = UserProfile(
    name = fullName,
    handle = "@" + username.ifBlank { email.substringBefore("@") },
    location = location,
    bio = bio,
    reviewsCount = reviewsCount,
    followersDisplay = "$followersCount",
    followingDisplay = "$followingCount"
)

fun User.toUserSearchResult(): UserSearchResult = UserSearchResult(
    uid = id,
    name = fullName,
    handle = "@" + username.ifBlank { email.substringBefore("@") },
    avatarUrl = avatarUrl
)

fun Notification.toNotificationUi(): NotificationUi {
    val vehicleName = if (!vehicleBrand.isNullOrBlank()) "$vehicleBrand $vehicleModel" else null

    val message = when (type) {
        NotificationType.LIKE ->
            if (vehicleName != null) "le dio like a tu reseña del $vehicleName" else "le dio like a tu reseña"
        NotificationType.REVIEW ->
            if (vehicleName != null) "también dejó una reseña en el $vehicleName" else "también dejó una reseña en un carro que reseñaste"
        NotificationType.FOLLOW -> "empezó a seguirte"
        NotificationType.COMMENT, NotificationType.RATING -> ""
    }

    val section = createdAt?.let { iso ->
        try {
            val hours = Duration.between(Instant.parse(iso), Instant.now()).toHours()
            when {
                hours < 24 -> "HOY"
                hours < 24 * 7 -> "ESTA SEMANA"
                else -> "ANTES"
            }
        } catch (e: Exception) {
            "ANTES"
        }
    } ?: "ANTES"

    return NotificationUi(
        id = id,
        type = type,
        actorName = actorName,
        actorAvatarUrl = actorAvatarUrl,
        message = message,
        timeAgo = createdAt.toRelativeTimeEs(),
        section = section,
        thumbnailUrl = vehicleImageUrl
    )
}
