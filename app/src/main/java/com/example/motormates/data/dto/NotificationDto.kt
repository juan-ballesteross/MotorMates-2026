package com.example.motormates.data.dto

import com.google.firebase.firestore.DocumentId

/**
 * Documento de la subcolección "notifications" de cada usuario. Solo
 * guarda ids (actor, vehículo, reseña): el nombre del actor y los datos
 * del vehículo se resuelven al leer, así siempre se ven con su valor
 * actual (igual que ya pasa con el nombre en las listas de seguidores).
 */
data class NotificationDto(
    @DocumentId val id: String = "",
    val type: String = "",
    val actorId: String = "",
    val vehicleId: String? = null,
    val reviewId: String? = null,
    val createdAt: Long = 0L
)
