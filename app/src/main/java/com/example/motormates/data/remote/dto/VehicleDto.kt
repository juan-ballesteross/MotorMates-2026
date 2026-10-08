package com.example.motormates.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Lo que manda el backend en GET /vehicles y GET /vehicles/{id}.
 * Es un objeto de transferencia: no se usa en la UI, se mapea a Vehicle
 * (ver DtoMappers.kt) para que un cambio en la BD no rompa las pantallas.
 */
data class VehicleDto(
    @SerializedName("id") val id: Int,
    @SerializedName("brand") val brand: String,
    @SerializedName("model") val model: String,
    @SerializedName("year") val year: Int,
    @SerializedName("category") val category: String,
    @SerializedName("imageUrl") val imageUrl: String?,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("updatedAt") val updatedAt: String?
)
