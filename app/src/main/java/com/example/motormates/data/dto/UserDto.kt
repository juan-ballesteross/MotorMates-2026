package com.example.motormates.data.dto

import com.example.motormates.data.model.User
import com.google.firebase.firestore.DocumentId

/**
 * Perfil tal como lo guarda Firestore (users/{uid}) y como lo responde el
 * backend propio, que usa la misma forma. Todos los campos tienen valor por
 * defecto porque Firestore necesita un constructor sin argumentos.
 */
data class UserDto(
    @DocumentId val id: String = "",
    val fullName: String = "",
    val email: String = "",
    val username: String = "",
    val location: String = "",
    val bio: String = "",
    val avatarUrl: String? = null, // URL (String), no un recurso local
    val coverUrl: String? = null,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val createdAt: Long = 0L
) {
    fun toDomain(): User = User(
        id = id,
        fullName = fullName,
        email = email,
        username = username,
        location = location,
        bio = bio,
        avatarUrl = avatarUrl,
        coverUrl = coverUrl,
        followersCount = followersCount,
        followingCount = followingCount
    )
}
