package com.example.motormates.data.remote.datasource

import com.example.motormates.data.dto.UserDto
import com.example.motormates.data.remote.dto.FollowRequest
import com.example.motormates.data.remote.pollingFlow
import com.example.motormates.data.remote.service.UserApiService
import com.example.motormates.data.source.FollowDataSource
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException

/**
 * Seguidores contra el backend propio. El backend evita los follows
 * duplicados con su llave compuesta y calcula los contadores con COUNT,
 * así que aquí no hace falta una transacción como en Firestore.
 */
class FollowRetrofitDataSource @Inject constructor(
    private val service: UserApiService
) : FollowDataSource {

    override suspend fun followUser(followerUid: String, targetUid: String) {
        service.follow(targetUid, FollowRequest(followerId = followerUid))
    }

    override suspend fun unfollowUser(followerUid: String, targetUid: String) {
        service.unfollow(targetUid, followerUid)
    }

    /** El backend responde 404 cuando no lo sigue. */
    override suspend fun isFollowing(followerUid: String, targetUid: String): Boolean =
        try {
            service.checkFollowing(targetUid, followerUid)
            true
        } catch (e: HttpException) {
            if (e.code() == HTTP_NOT_FOUND) false else throw e
        }

    override fun observeFollowingIds(uid: String): Flow<List<String>> =
        pollingFlow { service.getFollowing(uid).map { it.id } }

    override fun observeFollowers(uid: String): Flow<List<UserDto>> =
        pollingFlow { service.getFollowers(uid) }

    override fun observeFollowing(uid: String): Flow<List<UserDto>> =
        pollingFlow { service.getFollowing(uid) }

    private companion object {
        const val HTTP_NOT_FOUND = 404
    }
}
