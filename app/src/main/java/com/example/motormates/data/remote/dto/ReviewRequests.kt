package com.example.motormates.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Cuerpo de POST /reviews. Los ids van como texto: userId es el uid de
 * Firebase y vehicleId lo convierte Postgres al buscar por llave primaria.
 */
data class CreateReviewRequest(
    @SerializedName("userId") val userId: String,
    @SerializedName("vehicleId") val vehicleId: String,
    @SerializedName("rating") val rating: Int,
    @SerializedName("comment") val comment: String?
)

/**
 * Cuerpo de PUT /reviews/{id}. Solo rating y comment son editables.
 * Van nulables a propósito: Gson omite los campos null, y el controlador
 * solo toca los campos presentes (comment !== undefined), así que un null
 * aquí significa "no cambiar este campo".
 */
data class UpdateReviewRequest(
    @SerializedName("rating") val rating: Int?,
    @SerializedName("comment") val comment: String?
)

/** Cuerpo de POST /reviews/{id}/likes. */
data class LikeRequest(
    @SerializedName("userId") val userId: String
)
