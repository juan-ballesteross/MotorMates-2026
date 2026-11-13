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
    imageUrl = imageUrl,
    ownerId = ownerId,
    ownerName = if (ownerId != null) (ownerName?.ifBlank { null } ?: "Usuario") else null,
    ownerAvatarUrl = ownerAvatarUrl
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
    traccion = traccion ?: "—",
    ownerId = ownerId,
    ownerName = if (ownerId != null) (ownerName?.ifBlank { null } ?: "Usuario") else null,
    ownerAvatarUrl = ownerAvatarUrl
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
    isLikedByMe = id in likedReviewIds,
    lugarTexto = location?.placeName,
    imageUrls = imageUrls
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
    isLikedByMe = id in likedReviewIds,
    lugarTexto = location?.placeName
)

/**
 * La reseña como marcador del mapa, o null si no tiene ubicación.
 *
 * Es el único mapper que devuelve null, y es a propósito: la regla "sin
 * ubicación no hay marcador" queda en un solo lugar y se puede probar
 * directamente, en vez de repartida en filtros por la UI.
 *
 * vehicleImageUrl llega por parámetro por la misma razón que en
 * toUserReviewUi: la reseña no trae la foto del carro, la resuelve el
 * ViewModel con el catálogo.
 */
fun Review.toReviewMapMarkerUi(vehicleImageUrl: String? = null): ReviewMapMarkerUi? {
    val location = location ?: return null
    return ReviewMapMarkerUi(
        id = id,
        userId = userId,
        vehicleId = vehicleId,
        lat = location.lat,
        lng = location.lng,
        // Mismos respaldos que toFeedReviewUi: la copia del autor que la
        // reseña trae dentro puede venir vacía.
        nombreUsuario = author?.fullName?.ifBlank { null } ?: "Usuario",
        avatarUrl = author?.avatarUrl,
        vehicleName = vehicle?.let { "${it.brand} ${it.model}" } ?: "Vehículo",
        tiempoTexto = createdAt.toRelativeTimeEs(),
        calificacion = rating.coerceIn(0, 5),
        comentario = comment.orEmpty(),
        lugarTexto = location.placeName,
        vehicleImageUrl = vehicleImageUrl?.ifBlank { null },
        likesCount = likesCount.coerceAtLeast(0)
    )
}

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
    timeAgo = createdAt.toRelativeTimeEs(),
    lugarTexto = location?.placeName
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
        NotificationType.LIKE_STORY -> "le dio like a tu historia"
        NotificationType.MESSAGE -> "te envió un mensaje"
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
        actorId = actorId,
        actorName = actorName,
        actorAvatarUrl = actorAvatarUrl,
        message = message,
        timeAgo = createdAt.toRelativeTimeEs(),
        section = section,
        thumbnailUrl = vehicleImageUrl
    )
}

fun Story.toStoryUser(): StoryUser = StoryUser(
    id = id,
    userId = userId,
    name = userName,
    avatarUrl = userAvatarUrl,
    imageUrl = imageUrl,
    timeAgo = createdAt.toRelativeTimeEs()
)
