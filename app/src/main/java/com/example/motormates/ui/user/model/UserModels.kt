package com.example.motormates.ui.user.model

import com.example.motormates.R

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

val sampleUserProfile = UserProfile(
    name = "Rodrigo Salinas",
    handle = "@rodrigo.drives",
    location = "Ciudad de México",
    bio = "Coleccionista de deportivos alemanes. Fines de semana en el circuito.",
    reviewsCount = 47,
    followersDisplay = "1.2K",
    followingCount = 186
)

val sampleUserCars = listOf(
    GarageCar("911 GT3", R.drawable.gt3),
    GarageCar("M3 Competition", R.drawable.m3)
)
