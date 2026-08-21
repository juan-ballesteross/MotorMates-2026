package com.example.motormates.data.model

/**
 * Datos de perfil de usuario mostrados en la pantalla de Perfil.
 */
data class UserProfile(
    val name: String,
    val handle: String,
    val location: String,
    val bio: String,
    val reviewsCount: Int,
    val followersDisplay: String,
    val followingCount: Int
)

data class GarageCar(val name: String, val imageRes: Int)

enum class ProfileTab { REVIEWS, GARAGE, ACTIVITY }
