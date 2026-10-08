package com.example.motormates.data.model

/**
 * Modelo de dominio de una reseña. author y vehicle son nulables porque
 * cada endpoint del backend incluye solo uno de los dos.
 */
data class Review(
    val id: Int,
    val rating: Int,
    val comment: String?,
    val userId: Int,
    val vehicleId: Int,
    val createdAt: String?,
    val author: ReviewAuthor?,
    val vehicle: ReviewVehicleRef?
)

data class ReviewAuthor(val id: Int, val fullName: String)

data class ReviewVehicleRef(val id: Int, val brand: String, val model: String)
