package com.example.motormates.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Cuerpo de POST /reviews. */
data class CreateReviewRequest(
    @SerializedName("userId") val userId: Int,
    @SerializedName("vehicleId") val vehicleId: Int,
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
