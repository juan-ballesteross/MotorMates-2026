package com.example.motormates.data.dto

import com.google.firebase.firestore.DocumentId

/**
 * Documento de las subcolecciones "followers"/"following" de un usuario.
 * Su id es el uid del otro usuario (quien sigue, o a quien se sigue),
 * según en cuál de las dos subcolecciones viva.
 */
data class FollowDto(
    @DocumentId val id: String = "",
    val createdAt: Long = 0L
)
