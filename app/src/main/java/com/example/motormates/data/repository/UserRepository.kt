package com.example.motormates.data.repository

import com.example.motormates.data.model.User
import com.example.motormates.data.source.UserDataSource
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

interface UserRepository {
    /** Perfil del usuario autenticado (uid de Firebase Auth). */
    suspend fun getCurrentUserProfile(uid: String): Result<User>

    /** El mismo perfil en tiempo real: contadores, avatar y portada al día. */
    fun observeUserProfile(uid: String): Flow<Result<User>>

    /**
     * success(true/false) solo cuando se pudo comprobar contra la fuente de
     * datos; failure si no se pudo (sin red). Quien decida cerrar la sesión
     * necesita esa diferencia para no sacar a nadie por un problema de conexión.
     */
    suspend fun hasProfile(uid: String): Result<Boolean>

    /**
     * Valida y guarda los datos editables del perfil. El username se guarda
     * normalizado (sin espacios y en minúsculas) y debe ser único.
     */
    suspend fun updateCurrentUserProfile(
        uid: String,
        fullName: String,
        username: String,
        location: String,
        bio: String
    ): Result<Unit>

    /** Todos los perfiles, para el buscador de usuarios. */
    suspend fun getAllRegisteredUsers(): Result<List<User>>

    suspend fun updateCurrentUserAvatar(uid: String, avatarUrl: String): Result<Unit>

    suspend fun updateCurrentUserCover(uid: String, coverUrl: String): Result<Unit>
}

class UserRepositoryImpl @Inject constructor(
    private val userDataSource: UserDataSource
) : UserRepository {

    override suspend fun getCurrentUserProfile(uid: String): Result<User> {
        return try {
            val user = userDataSource.getUser(uid)
            if (user != null) {
                Result.success(user.toDomain())
            } else {
                Result.failure(Exception(PROFILE_NOT_FOUND))
            }
        } catch (e: Exception) {
            Result.failure(Exception(PROFILE_LOAD_ERROR))
        }
    }

    override suspend fun hasProfile(uid: String): Result<Boolean> {
        return try {
            Result.success(userDataSource.getUser(uid) != null)
        } catch (e: Exception) {
            Result.failure(Exception(PROFILE_LOAD_ERROR))
        }
    }

    override fun observeUserProfile(uid: String): Flow<Result<User>> =
        userDataSource.observeUser(uid)
            .map { user ->
                if (user != null) Result.success(user.toDomain())
                else Result.failure(Exception(PROFILE_NOT_FOUND))
            }
            .catch { emit(Result.failure(Exception(PROFILE_LOAD_ERROR))) }

    override suspend fun updateCurrentUserProfile(
        uid: String,
        fullName: String,
        username: String,
        location: String,
        bio: String
    ): Result<Unit> {
        val cleanName = fullName.trim()
        val cleanUsername = username.trim().removePrefix("@").lowercase()

        if (cleanName.isEmpty()) {
            return Result.failure(Exception("El nombre no puede estar vacío"))
        }
        if (!USERNAME_REGEX.matches(cleanUsername)) {
            return Result.failure(
                Exception("El usuario solo puede tener letras, números, punto y guion bajo (3–20)")
            )
        }

        return try {
            if (userDataSource.isUsernameTaken(cleanUsername, exceptUid = uid)) {
                return Result.failure(Exception("Ese nombre de usuario ya está en uso"))
            }
            userDataSource.updateUser(uid, cleanName, cleanUsername, location.trim(), bio.trim())
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(Exception(NO_CONNECTION_MESSAGE))
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo guardar tu perfil"))
        }
    }

    override suspend fun getAllRegisteredUsers(): Result<List<User>> {
        return try {
            Result.success(userDataSource.getAllUsers().map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo cargar la lista de usuarios"))
        }
    }

    override suspend fun updateCurrentUserAvatar(uid: String, avatarUrl: String): Result<Unit> {
        return try {
            userDataSource.updateAvatarUrl(uid, avatarUrl)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo guardar la foto en tu perfil"))
        }
    }

    override suspend fun updateCurrentUserCover(uid: String, coverUrl: String): Result<Unit> {
        return try {
            userDataSource.updateCoverUrl(uid, coverUrl)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo guardar la foto de portada"))
        }
    }

    private companion object {
        const val PROFILE_NOT_FOUND = "No se encontró tu perfil"
        const val PROFILE_LOAD_ERROR = "No se pudo cargar tu perfil"
        val USERNAME_REGEX = Regex("^[a-z0-9._]{3,20}$")
    }
}
