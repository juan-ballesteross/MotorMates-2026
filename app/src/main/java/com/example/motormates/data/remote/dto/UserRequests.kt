package com.example.motormates.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Cuerpo de POST /users: el id es el uid de Firebase Auth. */
data class CreateUserRequest(
    @SerializedName("id") val id: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("email") val email: String,
    @SerializedName("username") val username: String
)

/**
 * Cuerpo de PATCH /users/{id}. Igual que UpdateReviewRequest: Gson omite
 * los null y el backend solo cambia los campos presentes, así que null
 * significa "no cambiar este campo".
 */
data class UpdateUserRequest(
    @SerializedName("fullName") val fullName: String? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("location") val location: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("avatarUrl") val avatarUrl: String? = null,
    @SerializedName("coverUrl") val coverUrl: String? = null
)

/** Cuerpo de POST /users/{id}/followers: followerId empieza a seguir a {id}. */
data class FollowRequest(
    @SerializedName("followerId") val followerId: String
)

/** Cuerpo de POST /users/{id}/notifications. */
data class CreateNotificationRequest(
    @SerializedName("type") val type: String,
    @SerializedName("actorId") val actorId: String,
    @SerializedName("vehicleId") val vehicleId: String?,
    @SerializedName("reviewId") val reviewId: String?
)
