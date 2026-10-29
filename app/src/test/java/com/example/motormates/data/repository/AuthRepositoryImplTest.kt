package com.example.motormates.data.repository

import com.example.motormates.data.dto.UserDto
import com.example.motormates.data.model.AuthUser
import com.example.motormates.data.source.AuthDataSource
import com.example.motormates.data.source.UserDataSource
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
            FakeAuthDataSource(signInResult = Result.success(expectedUser)),
            FakeUserDataSource(),
            FakePushTokenRepository()
        )

        val result = repository.signIn(" jose@example.com ", "secret")

        assertEquals(expectedUser, result.getOrThrow())
    }

    @Test
    fun signInFailureIsPropagated() = runTest {
        val repository = AuthRepositoryImpl(
            FakeAuthDataSource(signInResult = Result.failure(IllegalArgumentException("invalid credentials"))),
            FakeUserDataSource(),
            FakePushTokenRepository()
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
            FakeAuthDataSource(currentUser = currentUser),
            FakeUserDataSource(),
            FakePushTokenRepository()
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
        val repository = AuthRepositoryImpl(dataSource, FakeUserDataSource(), FakePushTokenRepository())

        assertEquals(dataSource.authState, repository.authState)
    }
}

internal class FakeAuthDataSource(
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

    override suspend fun deleteCurrentUser() {
        session.value = null
    }
}

/** UserDataSource en memoria, compartido por las pruebas de repositorios. */
internal class FakeUserDataSource(
    users: List<UserDto> = emptyList()
) : UserDataSource {

    private val usersById = users.associateBy { it.id }.toMutableMap()

    override suspend fun createUser(user: UserDto) {
        usersById[user.id] = user
    }

    override suspend fun getUser(uid: String): UserDto? = usersById[uid]

    override suspend fun updateUser(uid: String, fullName: String, bio: String) {
        usersById[uid]?.let { usersById[uid] = it.copy(fullName = fullName, bio = bio) }
    }

    override suspend fun updateAvatarUrl(uid: String, avatarUrl: String) {
        usersById[uid]?.let { usersById[uid] = it.copy(avatarUrl = avatarUrl) }
    }

    override suspend fun updateCoverUrl(uid: String, coverUrl: String) {
        usersById[uid]?.let { usersById[uid] = it.copy(coverUrl = coverUrl) }
    }

    override suspend fun getAllUsers(): List<UserDto> = usersById.values.toList()
}

/** El registro del token no se prueba acá: solo hace falta para construir el repositorio. */
internal class FakePushTokenRepository : PushTokenRepository {
    override suspend fun registerCurrentDevice() = Unit
    override suspend fun unregisterCurrentDevice() = Unit
    override suspend fun saveToken(token: String) = Unit
}
