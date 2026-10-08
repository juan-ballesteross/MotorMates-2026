package com.example.motormates.data.model

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
 * Las especificaciones técnicas (potencia, 0-100, etc.) no existen en el
 * backend, así que se muestran como "—" en vez de inventarlas.
 */
fun Vehicle.toCarDetailUi(reviews: List<Review>): CarDetailUi = CarDetailUi(
    marca = brand.uppercase(),
    modelo = model,
    anio = year,
    categoria = category,
    imageUrl = imageUrl,
    calificacion = if (reviews.isEmpty()) 0f else reviews.map { it.rating }.average().toFloat(),
    numeroResenas = reviews.size,
    potencia = "—",
    aceleracion = "—",
    velocidadMaxima = "—",
    traccion = "—"
)

fun Vehicle.toReviewCarSummary(): ReviewCarSummary = ReviewCarSummary(
    title = "$brand $model",
    year = year,
    categoryLabel = category,
    imageUrl = imageUrl
)

fun Review.toReviewUi(currentUserId: Int = CURRENT_USER_ID): ReviewUi = ReviewUi(
    id = id,
    userId = userId,
    nombreUsuario = author?.fullName ?: "Usuario #$userId",
    avatarUrl = null,
    tiempoTexto = createdAt.toRelativeTimeEs(),
    calificacion = rating.coerceIn(0, 5),
    comentario = comment.orEmpty(),
    isMine = userId == currentUserId
)

fun Review.toUserReviewUi(): UserReviewUi = UserReviewUi(
    id = id,
    vehicleId = vehicleId,
    vehicleName = vehicle?.let { "${it.brand} ${it.model}" } ?: "Vehículo #$vehicleId",
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
