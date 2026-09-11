package com.example.motormates.data.source

import com.example.motormates.data.model.AuthUser
import kotlinx.coroutines.flow.Flow

interface AuthDataSource {
    val authState: Flow<AuthUser?>

    suspend fun signIn(email: String, password: String): AuthUser

    suspend fun signUp(fullName: String, email: String, password: String): AuthUser

    suspend fun signOut()

    fun getCurrentUser(): AuthUser?
}
