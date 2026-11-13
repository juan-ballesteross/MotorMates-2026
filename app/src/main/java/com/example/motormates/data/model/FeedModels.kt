package com.example.motormates.data.model

data class StoryUser(
    val id: String,
    val userId: String,
    val name: String,
    val avatarUrl: String? = null,
    val imageUrl: String? = null,
    val caption: String? = null,
    val timeAgo: String = ""
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

/**
 * Las tres vistas del Home: el catálogo de carros, lo que publica la gente que
 * sigo, o el mapa con las reseñas de las últimas 24 horas.
 */
enum class FeedTab { EXPLORAR, SIGUIENDO, MAPA }

/**
 * Una reseña lista para pintarse como marcador en el mapa. Vive junto a
 * FeedTab porque el mapa es una de las pestañas del Home.
 *
 * lat/lng van como Double y no como el LatLng de Maps a propósito: este
 * paquete lo consumen pruebas unitarias de JVM puro, y LatLng arrastraría el
 * SDK de Android. La conversión ocurre en la capa de UI.
 */
data class ReviewMapMarkerUi(
    val id: String,
    val userId: String,
    val vehicleId: String,
    val lat: Double,
    val lng: Double,
    val nombreUsuario: String,
    val avatarUrl: String? = null,
    val vehicleName: String,
    val tiempoTexto: String,
    val calificacion: Int,
    val comentario: String,
    /** Dónde se publicó, ya legible. Null si no se le pudo resolver nombre. */
    val lugarTexto: String? = null,
    /** Foto del carro (catálogo). Es la imagen del marcador; null = ícono de respaldo. */
    val vehicleImageUrl: String? = null,
    val likesCount: Int = 0
)

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
    val isLikedByMe: Boolean = false,
    /** Dónde se publicó, ya legible. Null si no tiene ubicación o nombre. */
    val lugarTexto: String? = null
)
