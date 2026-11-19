package com.example.motormates.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Cuerpo de POST /vehicles. Las especificaciones son opcionales: Gson omite
 * los campos null y el backend las deja vacías.
 */
data class CreateVehicleRequest(
    @SerializedName("brand") val brand: String,
    @SerializedName("model") val model: String,
    @SerializedName("year") val year: Int,
    @SerializedName("category") val category: String,
    @SerializedName("imageUrl") val imageUrl: String?,
    @SerializedName("potencia") val potencia: String?,
    @SerializedName("aceleracion") val aceleracion: String?,
    @SerializedName("velocidadMaxima") val velocidadMaxima: String?,
    @SerializedName("traccion") val traccion: String?,
    @SerializedName("ownerId") val ownerId: String,
    @SerializedName("ownerName") val ownerName: String,
    @SerializedName("ownerAvatarUrl") val ownerAvatarUrl: String?
)

/** Cuerpo de PUT /vehicles/{id}. El dueño no viaja: no se puede cambiar. */
data class UpdateVehicleRequest(
    @SerializedName("brand") val brand: String,
    @SerializedName("model") val model: String,
    @SerializedName("year") val year: Int,
    @SerializedName("category") val category: String,
    @SerializedName("imageUrl") val imageUrl: String?,
    @SerializedName("potencia") val potencia: String?,
    @SerializedName("aceleracion") val aceleracion: String?,
    @SerializedName("velocidadMaxima") val velocidadMaxima: String?,
    @SerializedName("traccion") val traccion: String?
)
