package com.example.motormates.data.model

import com.example.motormates.data.dto.UserDto
import com.example.motormates.data.util.toRelativeTimeEs

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
fun Review.toReviewUi(currentUserId: String?): ReviewUi = ReviewUi(
    id = id,
    userId = userId,
    nombreUsuario = author?.fullName?.ifBlank { null } ?: "Usuario",
    avatarUrl = author?.avatarUrl,
    tiempoTexto = createdAt.toRelativeTimeEs(),
    calificacion = rating.coerceIn(0, 5),
    comentario = comment.orEmpty(),
    isMine = userId == currentUserId
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

/**
 * El backend solo guarda nombre y correo del usuario. El resto de los
 * campos que pide UserProfile se sintetizan: el handle sale del correo y
 * ubicación/bio quedan vacíos (ProfileInfoSection omite los campos en
 * blanco). reviewsCount se calcula con las reseñas consultadas, porque
 * tampoco es un dato que el backend devuelva.
 */
fun BackendUser.toUserProfile(reviewsCount: Int): UserProfile = UserProfile(
    name = fullName,
    handle = "@" + email.substringBefore("@"),
    location = "",
    bio = "",
    reviewsCount = reviewsCount,
    followersDisplay = "0",
    followingDisplay = "0"
)

/**
 * Perfil del usuario autenticado con Firebase, leído desde Firestore.
 * A diferencia de BackendUser, aquí sí hay location/bio/username propios.
 */
fun UserDto.toUserProfile(reviewsCount: Int): UserProfile = UserProfile(
    name = fullName,
    handle = "@" + username.ifBlank { email.substringBefore("@") },
    location = location,
    bio = bio,
    reviewsCount = reviewsCount,
    followersDisplay = "0",
    followingDisplay = "0"
)

fun UserDto.toUserSearchResult(): UserSearchResult = UserSearchResult(
    uid = id,
    name = fullName,
    handle = "@" + username.ifBlank { email.substringBefore("@") },
    avatarUrl = avatarUrl
)
