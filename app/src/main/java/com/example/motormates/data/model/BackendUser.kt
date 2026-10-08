package com.example.motormates.data.model

/**
 * Usuario del backend propio. Se llama BackendUser y no User para no
 * confundirlo con AuthUser, que es el usuario de Firebase Auth.
 */
data class BackendUser(
    val id: Int,
    val fullName: String,
    val email: String
)
