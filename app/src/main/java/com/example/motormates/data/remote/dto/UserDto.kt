package com.example.motormates.data.remote.dto

import com.google.gson.annotations.SerializedName

/** GET /users/{id}. El backend solo guarda nombre y correo. */
data class UserDto(
    @SerializedName("id") val id: Int,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("email") val email: String,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("updatedAt") val updatedAt: String?
)
