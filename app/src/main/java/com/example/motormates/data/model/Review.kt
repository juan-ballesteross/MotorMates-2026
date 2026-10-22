package com.example.motormates.data.model

/**
 * Modelo de dominio de una reseña. Los ids son String porque en Firestore
 * el id de un documento es un texto (y el userId es el uid de Firebase Auth).
 * author y vehicle son nulables: son la "captura" desnormalizada que se
 * guarda dentro de la reseña, y una fuente de datos puede no traerla.
 */
data class Review(
    val id: String,
    val rating: Int,
    val comment: String?,
    val userId: String,
    val vehicleId: String,
    val createdAt: String?,
    val likesCount: Int = 0,
    val author: ReviewAuthor?,
    val vehicle: ReviewVehicleRef?
)

data class ReviewAuthor(val id: String, val fullName: String, val avatarUrl: String? = null)

data class ReviewVehicleRef(val id: String, val brand: String, val model: String)

/**
 * Lo que el repositorio le entrega a la fuente de datos para crear una
 * reseña: además de los datos propios, lleva el nombre/foto del autor y la
 * marca/modelo del vehículo para guardarlos desnormalizados (sin JOINs).
 */
data class NewReview(
    val userId: String,
    val vehicleId: String,
    val rating: Int,
    val comment: String?,
    val authorName: String,
    val authorAvatarUrl: String?,
    val vehicleBrand: String,
    val vehicleModel: String
)
