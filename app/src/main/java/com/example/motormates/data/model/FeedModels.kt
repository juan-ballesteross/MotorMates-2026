package com.example.motormates.data.model

data class StoryUser(
    val userId: String,
    val name: String,
    val avatarResId: Int,
    val imageRes: Int? = null,
    val caption: String? = null
)

data class ReviewPost(
    val userName: String,
    val avatarResId: Int,
    val carName: String,
    val imageResId: Int,
    val timeAgo: String,
    val rating: Int,
    val caption: String,
    val likes: Int,
    val comments: Int,
    val shares: Int
)

/**
 * Un vehículo listo para pintar en la tarjeta del Home. Se mantiene aparte
 * del modelo de dominio Vehicle para que la pantalla no dependa de cómo
 * viene armado el dato en la capa de datos.
 */
data class FeedVehicleUi(
    val id: String,
    val brand: String,
    val model: String,
    val year: Int,
    val category: String,
    val imageUrl: String?
)

/** Las dos vistas del Home: el catálogo de carros o lo que publica la gente que sigo. */
enum class FeedTab { EXPLORAR, SIGUIENDO }

/**
 * Una reseña lista para pintar en el feed de "Siguiendo". Es aparte de
 * ReviewUi porque acá importan las dos cosas a la vez: quién la escribió
 * (en el detalle del carro el autor es lo único que varía) y de qué carro
 * habla (en el perfil el autor ya se sabe).
 */
data class FeedReviewUi(
    val id: String,
    val userId: String,
    val vehicleId: String,
    val nombreUsuario: String,
    val avatarUrl: String? = null,
    val vehicleName: String,
    val vehicleImageUrl: String? = null,
    val tiempoTexto: String,
    val calificacion: Int,
    val comentario: String,
    val likesCount: Int = 0,
    val isLikedByMe: Boolean = false
)
