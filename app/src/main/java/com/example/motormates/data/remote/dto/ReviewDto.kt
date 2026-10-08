package com.example.motormates.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Review tal como la devuelve el backend. Los dos objetos anidados son
 * excluyentes y por eso van nulables: GET /vehicles/{id}/reviews incluye
 * solo "user", GET /users/{id}/reviews incluye solo "vehicle", y el POST
 * y el PUT no incluyen ninguno.
 */
data class ReviewDto(
    @SerializedName("id") val id: Int,
    @SerializedName("rating") val rating: Int,
    @SerializedName("comment") val comment: String?,
    @SerializedName("userId") val userId: Int,
    @SerializedName("vehicleId") val vehicleId: Int,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("updatedAt") val updatedAt: String?,
    @SerializedName("user") val user: ReviewUserDto? = null,
    @SerializedName("vehicle") val vehicle: ReviewVehicleDto? = null
)

/**
 * Proyecciones parciales: el backend las limita con attributes, así que
 * tienen su propio DTO en vez de reusar UserDto/VehicleDto con la mitad
 * de los campos en null.
 */
data class ReviewUserDto(
    @SerializedName("id") val id: Int,
    @SerializedName("fullName") val fullName: String
)

data class ReviewVehicleDto(
    @SerializedName("id") val id: Int,
    @SerializedName("brand") val brand: String,
    @SerializedName("model") val model: String
)
