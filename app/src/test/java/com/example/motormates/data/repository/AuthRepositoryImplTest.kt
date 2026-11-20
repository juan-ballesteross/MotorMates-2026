package com.example.motormates.data.repository

import com.example.motormates.data.dto.UserDto
import com.example.motormates.data.model.AuthUser
import com.example.motormates.data.source.AuthDataSource
import com.example.motormates.data.source.UserDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
    fun deleteAccountClearsCurrentUser() = runTest {
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

        val result = repository.deleteAccount()

        assertTrue(result.isSuccess)
        assertEquals(null, repository.getCurrentUser())
    }

    @Test
    fun deleteAccountFailureIsPropagated() = runTest {
        val repository = AuthRepositoryImpl(
            FakeAuthDataSource(deleteCurrentUserError = IllegalStateException("boom")),
            FakeUserDataSource(),
            FakePushTokenRepository()
        )

        val result = repository.deleteAccount()

        assertTrue(result.isFailure)
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
    // ---------- signInWithGoogle ----------

    @Test
    fun signInWithGoogleCreatesProfileOnFirstSignIn() = runTest {
        val googleUser = AuthUser(id = "uid-g", email = "ana@gmail.com", displayName = "Ana Gomez")
        val users = FakeUserDataSource()
        val repository = AuthRepositoryImpl(
            FakeAuthDataSource(signInWithGoogleResult = Result.success(googleUser)),
            users,
            FakePushTokenRepository()
        )

        val result = repository.signInWithGoogle("id-token")

        assertEquals(googleUser, result.getOrThrow())
        val profile = users.getUser("uid-g")
        assertEquals("Ana Gomez", profile?.fullName)
        assertEquals("ana@gmail.com", profile?.email)
        // El username inicial es lo que va antes de la arroba, igual que en signUp.
        assertEquals("ana", profile?.username)
    }

    @Test
    fun signInWithGoogleKeepsExistingProfile() = runTest {
        val googleUser = AuthUser(id = "uid-g", email = "ana@gmail.com", displayName = "Ana Nueva")
        val users = FakeUserDataSource(
            listOf(UserDto(id = "uid-g", fullName = "Ana De Antes", email = "ana@gmail.com", username = "anita"))
        )
        val repository = AuthRepositoryImpl(
            FakeAuthDataSource(signInWithGoogleResult = Result.success(googleUser)),
            users,
            FakePushTokenRepository()
        )

        val result = repository.signInWithGoogle("id-token")

        assertTrue(result.isSuccess)
        // No se pisa el perfil que ya existia: el nombre y el username siguen.
        assertEquals("Ana De Antes", users.getUser("uid-g")?.fullName)
        assertEquals("anita", users.getUser("uid-g")?.username)
    }

    @Test
    fun signInWithGoogleUsesFallbackNameWhenGoogleGivesNone() = runTest {
        val googleUser = AuthUser(id = "uid-g", email = "sin.nombre@gmail.com", displayName = null)
        val users = FakeUserDataSource()
        val repository = AuthRepositoryImpl(
            FakeAuthDataSource(signInWithGoogleResult = Result.success(googleUser)),
            users,
            FakePushTokenRepository()
        )

        repository.signInWithGoogle("id-token")

        assertEquals("Usuario de Google", users.getUser("uid-g")?.fullName)
    }

    @Test
    fun signInWithGoogleFailureGivesAMessage() = runTest {
        val repository = AuthRepositoryImpl(
            FakeAuthDataSource(
                signInWithGoogleResult = Result.failure(IllegalStateException("token vencido"))
            ),
            FakeUserDataSource(),
            FakePushTokenRepository()
        )

        val result = repository.signInWithGoogle("id-token")

        assertTrue(result.isFailure)
        // El mensaje tecnico no se le muestra al usuario.
        assertEquals("Error al iniciar sesión con Google", result.exceptionOrNull()?.message)
    }
}

internal class FakeAuthDataSource(
    currentUser: AuthUser? = null,
    private val signInResult: Result<AuthUser> = Result.success(
        AuthUser(id = "user-1", email = "jose@example.com", displayName = "Jose")
    ),
    private val signUpResult: Result<AuthUser> = Result.success(
        AuthUser(id = "user-2", email = "ana@example.com", displayName = "Ana")
    ),
    private val deleteCurrentUserError: Throwable? = null,
    private val signInWithGoogleResult: Result<AuthUser> = Result.success(
        AuthUser(id = "user-google", email = "jose@gmail.com", displayName = "Jose Google")
    )
) : AuthDataSource {

    private val session = MutableStateFlow(currentUser)

    override val authState: Flow<AuthUser?> = session

    override suspend fun signIn(email: String, password: String): AuthUser {
        val user = signInResult.getOrThrow()
        session.value = user
        return user
    }

    override suspend fun signInWithGoogle(idToken: String): AuthUser {
        val user = signInWithGoogleResult.getOrThrow()
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
        deleteCurrentUserError?.let { throw it }
        session.value = null
    }

    override suspend fun updateDisplayName(name: String) {
        session.value = session.value?.copy(displayName = name)
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

    override suspend fun updateUser(uid: String, fullName: String, username: String, location: String, bio: String) {
        usersById[uid]?.let {
            usersById[uid] = it.copy(fullName = fullName, username = username, location = location, bio = bio)
        }
    }

    override suspend fun isUsernameTaken(username: String, exceptUid: String): Boolean =
        usersById.values.any { it.username == username && it.id != exceptUid }

    override suspend fun updateAvatarUrl(uid: String, avatarUrl: String) {
        usersById[uid]?.let { usersById[uid] = it.copy(avatarUrl = avatarUrl) }
    }

    override suspend fun updateCoverUrl(uid: String, coverUrl: String) {
        usersById[uid]?.let { usersById[uid] = it.copy(coverUrl = coverUrl) }
    }

    override suspend fun getAllUsers(): List<UserDto> = usersById.values.toList()

    override fun observeUser(uid: String): Flow<UserDto?> = flowOf(usersById[uid])
}

/** El registro del token no se prueba acá: solo hace falta para construir el repositorio. */
internal class FakePushTokenRepository : PushTokenRepository {
    override suspend fun registerCurrentDevice() = Unit
    override suspend fun unregisterCurrentDevice() = Unit
    override suspend fun saveToken(token: String) = Unit
}
