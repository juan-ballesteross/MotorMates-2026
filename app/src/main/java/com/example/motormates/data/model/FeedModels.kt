package com.example.motormates.data.model

data class StoryUser(
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
    val id: Int,
    val brand: String,
    val model: String,
    val year: Int,
    val category: String,
    val imageUrl: String?
)
