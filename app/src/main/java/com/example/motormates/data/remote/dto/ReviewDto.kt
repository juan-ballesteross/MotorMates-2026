package com.example.motormates.data.remote.dto

import com.example.motormates.data.dto.ReviewDtoGeneric
import com.example.motormates.data.model.Review
import com.example.motormates.data.remote.mapper.mapToDomain
import com.google.gson.annotations.SerializedName

/**
 * Review tal como la devuelve el backend. Todas las respuestas traen el
 * autor (con su foto), el vehículo y el likesCount, que el backend cuenta
 * en la misma consulta. userId es el uid de Firebase (texto); el id de la
 * review y el del vehículo siguen siendo numéricos en Postgres.
 */
data class ReviewDto(
    @SerializedName("id") val id: Int,
    @SerializedName("rating") val rating: Int,
    @SerializedName("comment") val comment: String?,
    @SerializedName("userId") val userId: String,
    @SerializedName("vehicleId") val vehicleId: Int,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("updatedAt") val updatedAt: String?,
    @SerializedName("likesCount") val likesCount: Int = 0,
    @SerializedName("user") val user: ReviewUserDto? = null,
    @SerializedName("vehicle") val vehicle: ReviewVehicleDto? = null
) : ReviewDtoGeneric {
    override fun toDomain(): Review = mapToDomain()
}

/**
 * Proyecciones parciales: el backend las limita con attributes, así que
 * tienen su propio DTO en vez de reusar UserDto/VehicleDto con la mitad
 * de los campos en null.
 */
data class ReviewUserDto(
    @SerializedName("id") val id: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("avatarUrl") val avatarUrl: String? = null
)

data class ReviewVehicleDto(
    @SerializedName("id") val id: Int,
    @SerializedName("brand") val brand: String,
    @SerializedName("model") val model: String
)
