package com.example.motormates.data.dto

import com.google.firebase.firestore.DocumentId

/** Documento de la colección "reviews". */
data class ReviewDto(
    @DocumentId val id: String = "",
    val userId: String = "",
    val vehicleId: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val likesCount: Int = 0,
    val createdAt: Long = 0L
)