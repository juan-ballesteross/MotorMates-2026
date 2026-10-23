package com.example.motormates.data.repository

import com.example.motormates.data.dto.UserDto
import com.example.motormates.data.source.FollowDataSource
import com.google.firebase.firestore.FirebaseFirestoreException
import javax.inject.Inject

interface FollowRepository {
    suspend fun followUser(followerUid: String, targetUid: String): Result<Unit>
    suspend fun unfollowUser(followerUid: String, targetUid: String): Result<Unit>
    suspend fun isFollowing(followerUid: String, targetUid: String): Result<Boolean>
    suspend fun getFollowers(uid: String): Result<List<UserDto>>
    suspend fun getFollowing(uid: String): Result<List<UserDto>>
}

class FollowRepositoryImpl @Inject constructor(
    private val followDataSource: FollowDataSource
) : FollowRepository {

    override suspend fun followUser(followerUid: String, targetUid: String): Result<Unit> {
        return try {
            followDataSource.followUser(followerUid, targetUid)
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(Exception(e.toFollowMessage()))
        } catch (e: Exception) {
            Result.failure(Exception("Ocurrió un error inesperado"))
        }
    }

    override suspend fun unfollowUser(followerUid: String, targetUid: String): Result<Unit> {
        return try {
            followDataSource.unfollowUser(followerUid, targetUid)
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(Exception(e.toFollowMessage()))
        } catch (e: Exception) {
            Result.failure(Exception("Ocurrió un error inesperado"))
        }
    }

    override suspend fun isFollowing(followerUid: String, targetUid: String): Result<Boolean> {
        return try {
            Result.success(followDataSource.isFollowing(followerUid, targetUid))
        } catch (e: Exception) {
            Result.success(false)
        }
    }

    override suspend fun getFollowers(uid: String): Result<List<UserDto>> {
        return try {
            Result.success(followDataSource.getFollowers(uid))
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo cargar la lista de seguidores"))
        }
    }

    override suspend fun getFollowing(uid: String): Result<List<UserDto>> {
        return try {
            Result.success(followDataSource.getFollowing(uid))
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo cargar la lista de seguidos"))
        }
    }

    private fun FirebaseFirestoreException.toFollowMessage(): String = when (code) {
        FirebaseFirestoreException.Code.UNAVAILABLE ->
            "Sin conexión a internet, verifica tu red"
        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "No tienes permiso para hacer esto"
        FirebaseFirestoreException.Code.NOT_FOUND ->
            "Este usuario ya no existe"
        else -> "No se pudo completar la acción, intenta de nuevo"
    }
}
