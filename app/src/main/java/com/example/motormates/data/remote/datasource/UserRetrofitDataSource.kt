package com.example.motormates.data.remote.datasource

import com.example.motormates.data.dto.UserDto
import com.example.motormates.data.remote.dto.CreateUserRequest
import com.example.motormates.data.remote.dto.UpdateUserRequest
import com.example.motormates.data.remote.pollingFlow
import com.example.motormates.data.remote.service.UserApiService
import com.example.motormates.data.source.UserDataSource
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException

/**
 * Perfiles contra el backend propio. El id es el mismo uid de Firebase Auth
 * que usa Firestore, así que el resto de la app no cambia nada.
 */
class UserRetrofitDataSource @Inject constructor(
    private val service: UserApiService
) : UserDataSource {

    override suspend fun createUser(user: UserDto) {
        service.createUser(
            CreateUserRequest(
                id = user.id,
                fullName = user.fullName,
                email = user.email,
                username = user.username
            )
        )
    }

    /** Un 404 es "no tiene perfil" (null), igual que un documento inexistente. */
    override suspend fun getUser(uid: String): UserDto? =
        try {
            service.getUser(uid)
        } catch (e: HttpException) {
            if (e.code() == HTTP_NOT_FOUND) null else throw e
        }

    override suspend fun updateUser(
        uid: String,
        fullName: String,
        username: String,
        location: String,
        bio: String
    ) {
        service.updateUser(
            uid,
            UpdateUserRequest(fullName = fullName, username = username, location = location, bio = bio)
        )
    }

    override suspend fun isUsernameTaken(username: String, exceptUid: String): Boolean =
        service.getUsers(username = username).any { it.id != exceptUid }

    override suspend fun updateAvatarUrl(uid: String, avatarUrl: String) {
        service.updateUser(uid, UpdateUserRequest(avatarUrl = avatarUrl))
    }

    override suspend fun updateCoverUrl(uid: String, coverUrl: String) {
        service.updateUser(uid, UpdateUserRequest(coverUrl = coverUrl))
    }

    override suspend fun getAllUsers(): List<UserDto> = service.getUsers()

    override fun observeUser(uid: String): Flow<UserDto?> = pollingFlow { getUser(uid) }

    private companion object {
        const val HTTP_NOT_FOUND = 404
    }
}
