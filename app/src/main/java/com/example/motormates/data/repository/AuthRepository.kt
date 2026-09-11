package com.example.motormates.data.repository

import com.example.motormates.data.model.AuthUser
import com.example.motormates.data.source.AuthDataSource
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val authState: Flow<AuthUser?>

    suspend fun signIn(email: String, password: String): Result<AuthUser>

    suspend fun signUp(fullName: String, email: String, password: String): Result<AuthUser>

    suspend fun signOut(): Result<Unit>

    fun getCurrentUser(): AuthUser?
}

class AuthRepositoryImpl @Inject constructor(
    private val authDataSource: AuthDataSource
) : AuthRepository {

    override val authState: Flow<AuthUser?> = authDataSource.authState

    override suspend fun signIn(email: String, password: String): Result<AuthUser> =
        runCatching { authDataSource.signIn(email.trim(), password) }

    override suspend fun signUp(fullName: String, email: String, password: String): Result<AuthUser> =
        runCatching { authDataSource.signUp(fullName.trim(), email.trim(), password) }

    override suspend fun signOut(): Result<Unit> =
        runCatching { authDataSource.signOut() }

    override fun getCurrentUser(): AuthUser? = authDataSource.getCurrentUser()
}
