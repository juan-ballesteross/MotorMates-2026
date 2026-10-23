package com.example.motormates.data.model

/**
 * Modelo de dominio de un perfil de usuario. Es lo que entregan los
 * repositorios a los ViewModels: el UserDto (con la forma del documento de
 * Firestore o de la respuesta del backend) se queda en la capa de datos.
 * El id es el uid de Firebase Auth.
 */
data class User(
    val id: String,
    val fullName: String,
    val email: String,
    val username: String,
    val location: String,
    val bio: String,
    val avatarUrl: String?,
    val coverUrl: String?,
    val followersCount: Int,
    val followingCount: Int
)
