package com.example.motormates.data.repository

import com.example.motormates.data.dto.UserDto
import com.example.motormates.data.model.BackendUser
import com.example.motormates.data.remote.datasource.UserRemoteDataSource
import com.example.motormates.data.remote.mapper.toDomain
import com.example.motormates.data.source.UserDataSource
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import retrofit2.HttpException

interface UserRepository {
    suspend fun getUserById(id: Int): Result<BackendUser>

    /** Perfil de Firestore del usuario autenticado (uid de Firebase Auth). */
    suspend fun getCurrentUserProfile(uid: String): Result<UserDto>

    /** El mismo perfil en tiempo real: contadores, avatar y portada al día. */
    fun observeUserProfile(uid: String): Flow<Result<UserDto>>

    /**
     * success(true/false) solo cuando se pudo comprobar contra Firestore;
     * failure si no se pudo (sin red). Quien decida cerrar la sesión necesita
     * esa diferencia para no sacar a nadie por un problema de conexión.
     */
    suspend fun hasProfile(uid: String): Result<Boolean>

    suspend fun updateCurrentUserProfile(uid: String, fullName: String, bio: String): Result<Unit>

    /** Todos los perfiles de Firestore, para el buscador de usuarios. */
    suspend fun getAllRegisteredUsers(): Result<List<UserDto>>

    suspend fun updateCurrentUserAvatar(uid: String, avatarUrl: String): Result<Unit>

    suspend fun updateCurrentUserCover(uid: String, coverUrl: String): Result<Unit>
}

class UserRepositoryImpl @Inject constructor(
    private val remoteDataSource: UserRemoteDataSource,
    private val userDataSource: UserDataSource
) : UserRepository {

    override suspend fun getUserById(id: Int): Result<BackendUser> {
        return try {
            Result.success(remoteDataSource.getUserById(id).toDomain())
        } catch (e: IOException) {
            Result.failure(Exception(NO_CONNECTION_MESSAGE))
        } catch (e: HttpException) {
            Result.failure(
                Exception(
                    e.toSpanishMessage(
                        notFound = "Este usuario no existe",
                        fallback = "No se pudo cargar el perfil"
                    )
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo cargar el perfil"))
        }
    }

    override suspend fun getCurrentUserProfile(uid: String): Result<UserDto> {
        return try {
            val user = userDataSource.getUser(uid)
            if (user != null) {
                Result.success(user)
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

    override fun observeUserProfile(uid: String): Flow<Result<UserDto>> =
        userDataSource.observeUser(uid)
            .map { user ->
                if (user != null) Result.success(user)
                else Result.failure(Exception(PROFILE_NOT_FOUND))
            }
            .catch { emit(Result.failure(Exception(PROFILE_LOAD_ERROR))) }

    override suspend fun updateCurrentUserProfile(
        uid: String,
        fullName: String,
        bio: String
    ): Result<Unit> {
        return try {
            userDataSource.updateUser(uid, fullName, bio)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo guardar tu perfil"))
        }
    }

    override suspend fun getAllRegisteredUsers(): Result<List<UserDto>> {
        return try {
            Result.success(userDataSource.getAllUsers())
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
    }
}
