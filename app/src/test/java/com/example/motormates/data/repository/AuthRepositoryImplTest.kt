package com.example.motormates.data.repository

import com.example.motormates.data.model.AuthUser
import com.example.motormates.data.source.AuthDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryImplTest {

    @Test
    fun signInReturnsAuthenticatedUser() = runTest {
        val expectedUser = AuthUser(
            id = "user-1",
            email = "jose@example.com",
            displayName = "Jose"
        )
        val repository = AuthRepositoryImpl(
            FakeAuthDataSource(signInResult = Result.success(expectedUser))
        )

        val result = repository.signIn(" jose@example.com ", "secret")

        assertEquals(expectedUser, result.getOrThrow())
    }

    @Test
    fun signInFailureIsPropagated() = runTest {
        val repository = AuthRepositoryImpl(
            FakeAuthDataSource(signInResult = Result.failure(IllegalArgumentException("invalid credentials")))
        )

        val result = repository.signIn("jose@example.com", "wrong")

        assertTrue(result.isFailure)
    }

    @Test
    fun signOutClearsCurrentUser() = runTest {
        val currentUser = AuthUser(
            id = "user-1",
            email = "jose@example.com",
            displayName = "Jose"
        )
        val repository = AuthRepositoryImpl(
            FakeAuthDataSource(currentUser = currentUser)
        )

        val result = repository.signOut()

        assertTrue(result.isSuccess)
        assertEquals(null, repository.getCurrentUser())
    }

    @Test
    fun authStateExposesSourceSessionFlow() {
        val currentUser = AuthUser(
            id = "user-1",
            email = "jose@example.com",
            displayName = "Jose"
        )
        val dataSource = FakeAuthDataSource(currentUser = currentUser)
        val repository = AuthRepositoryImpl(dataSource)

        assertEquals(dataSource.authState, repository.authState)
    }
}

private class FakeAuthDataSource(
    currentUser: AuthUser? = null,
    private val signInResult: Result<AuthUser> = Result.success(
        AuthUser(id = "user-1", email = "jose@example.com", displayName = "Jose")
    ),
    private val signUpResult: Result<AuthUser> = Result.success(
        AuthUser(id = "user-2", email = "ana@example.com", displayName = "Ana")
    )
) : AuthDataSource {

    private val session = MutableStateFlow(currentUser)

    override val authState: Flow<AuthUser?> = session

    override suspend fun signIn(email: String, password: String): AuthUser {
        val user = signInResult.getOrThrow()
        session.value = user
        return user
    }

    override suspend fun signUp(fullName: String, email: String, password: String): AuthUser {
        val user = signUpResult.getOrThrow()
        session.value = user
        return user
    }

    override suspend fun signOut() {
        session.value = null
    }

    override fun getCurrentUser(): AuthUser? = session.value
}
