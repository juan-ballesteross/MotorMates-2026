package com.example.motormates.data.dto

import com.google.firebase.firestore.DocumentId

/**
 * Documento de la subcolección "likes" de un review. Su id es el uid de
 * quien dio el like. userId se repite como campo para poder consultar
 * después los likes de un usuario.
 */
data class LikeDto(
    @DocumentId val id: String = "",
    val userId: String = "",
    val createdAt: Long = 0L
)