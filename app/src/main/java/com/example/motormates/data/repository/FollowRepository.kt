package com.example.motormates.data.repository

import com.example.motormates.data.dto.UserDto
import com.example.motormates.data.source.FollowDataSource
import com.google.firebase.firestore.FirebaseFirestoreException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

interface FollowRepository {
    suspend fun followUser(followerUid: String, targetUid: String): Result<Unit>
    suspend fun unfollowUser(followerUid: String, targetUid: String): Result<Unit>
    suspend fun isFollowing(followerUid: String, targetUid: String): Result<Boolean>
    fun observeFollowingIds(uid: String): Flow<List<String>>
    fun observeFollowers(uid: String): Flow<Result<List<UserDto>>>
    fun observeFollowing(uid: String): Flow<Result<List<UserDto>>>
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

    /** Mismo criterio que isFollowing: si falla, el feed se ve vacío pero no roto. */
    override fun observeFollowingIds(uid: String): Flow<List<String>> =
        followDataSource.observeFollowingIds(uid).catch { emit(emptyList()) }

    override fun observeFollowers(uid: String): Flow<Result<List<UserDto>>> =
        followDataSource.observeFollowers(uid)
            .map { Result.success(it) }
            .catch { emit(Result.failure(Exception("No se pudo cargar la lista de seguidores"))) }

    override fun observeFollowing(uid: String): Flow<Result<List<UserDto>>> =
        followDataSource.observeFollowing(uid)
            .map { Result.success(it) }
            .catch { emit(Result.failure(Exception("No se pudo cargar la lista de seguidos"))) }

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
